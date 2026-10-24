package com.estoque.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.estoque.domain.Location;
import com.estoque.domain.LocationUsage;
import com.estoque.domain.MoveState;
import com.estoque.domain.Product;
import com.estoque.domain.StockMove;
import com.estoque.domain.StockQuant;
import com.estoque.repository.StockMoveRepository;
import com.estoque.repository.StockQuantRepository;

/**
 * Núcleo do estoque: aplica movimentos aos saldos e calcula saldo e previsão.
 * Todas as quantidades por local consideram o local e seus sublocais.
 */
@Service
@Transactional
public class StockService {

    private final StockQuantRepository quants;
    private final StockMoveRepository moves;
    private final LocationService locationService;

    public StockService(StockQuantRepository quants, StockMoveRepository moves, LocationService locationService) {
        this.quants = quants;
        this.moves = moves;
        this.locationService = locationService;
    }

    @Transactional(readOnly = true)
    public BigDecimal onHand(Product product, Location location) {
        return quants.sumInSubtree(product.getId(), location.getFullName());
    }

    @Transactional(readOnly = true)
    public BigDecimal onHandTotal(Product product) {
        return quants.sumInternal(product.getId());
    }

    @Transactional(readOnly = true)
    public BigDecimal incoming(Product product, Location location) {
        return moves.sumIncoming(product.getId(), location.getFullName());
    }

    @Transactional(readOnly = true)
    public BigDecimal outgoing(Product product, Location location) {
        return moves.sumOutgoing(product.getId(), location.getFullName());
    }

    /** Previsão = saldo + entradas confirmadas - saídas confirmadas. */
    @Transactional(readOnly = true)
    public BigDecimal forecast(Product product, Location location) {
        return onHand(product, location).add(incoming(product, location)).subtract(outgoing(product, location));
    }

    /**
     * Conclui o movimento, atualizando os saldos. Saídas de locais internos consomem primeiro o
     * próprio local e depois os sublocais; falta de saldo gera erro.
     */
    public void applyMove(StockMove move) {
        if (move.getState() == MoveState.DONE || move.getState() == MoveState.CANCELLED) {
            throw new BusinessException("O movimento " + move.getReference() + " já foi " + move.getState().getLabel().toLowerCase() + ".");
        }
        var product = move.getProduct();
        var source = move.getSourceLocation();
        var dest = move.getDestLocation();
        BigDecimal quantity = move.getQuantity();

        if (source.isInternal()) {
            List<StockQuant> available = quants.findAvailableForUpdate(product.getId(), source.getFullName());
            BigDecimal total = available.stream().map(StockQuant::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
            if (total.compareTo(quantity) < 0) {
                throw new BusinessException("Estoque insuficiente de %s em %s: disponível %s, necessário %s."
                        .formatted(product.getDisplayName(), source.getFullName(), total.stripTrailingZeros().toPlainString(),
                                quantity.stripTrailingZeros().toPlainString()));
            }
            BigDecimal remaining = quantity;
            for (StockQuant quant : available) {
                BigDecimal take = quant.getQuantity().min(remaining);
                quant.setQuantity(quant.getQuantity().subtract(take));
                remaining = remaining.subtract(take);
                if (remaining.signum() == 0) {
                    break;
                }
            }
        }
        if (dest.isInternal()) {
            var quant = lockQuant(product, dest);
            quant.setQuantity(quant.getQuantity().add(quantity));
        }
        move.setState(MoveState.DONE);
        move.setDate(LocalDateTime.now());
    }

    /**
     * Ajuste de inventário: define a quantidade contada do produto exatamente neste local,
     * gerando um movimento de/para o local virtual de ajuste. Retorna {@code null} se não houver diferença.
     */
    public StockMove adjust(Product product, Location location, BigDecimal counted) {
        if (!location.isInternal()) {
            throw new BusinessException("Ajustes só podem ser feitos em locais internos.");
        }
        if (counted == null || counted.signum() < 0) {
            throw new BusinessException("A quantidade contada deve ser zero ou positiva.");
        }
        BigDecimal current = quants.findByProductIdAndLocationId(product.getId(), location.getId())
                .map(StockQuant::getQuantity).orElse(BigDecimal.ZERO);
        BigDecimal diff = counted.subtract(current);
        if (diff.signum() == 0) {
            return null;
        }
        var inventory = locationService.virtual(LocationUsage.INVENTORY);
        var move = diff.signum() > 0
                ? new StockMove(product, diff, inventory, location, "Ajuste de inventário")
                : new StockMove(product, diff.negate(), location, inventory, "Ajuste de inventário");
        move.setState(MoveState.CONFIRMED);
        applyMove(move);
        return moves.save(move);
    }

    private StockQuant lockQuant(Product product, Location location) {
        return quants.findForUpdate(product.getId(), location.getId())
                .orElseGet(() -> quants.save(new StockQuant(product, location)));
    }
}
