package com.estoque.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.estoque.domain.Location;
import com.estoque.domain.LocationUsage;
import com.estoque.domain.ManufacturingOrder;
import com.estoque.domain.MoveState;
import com.estoque.domain.Product;
import com.estoque.domain.ProductionState;
import com.estoque.domain.StockMove;
import com.estoque.repository.ManufacturingOrderRepository;
import com.estoque.repository.ProductRepository;
import com.estoque.repository.StockMoveRepository;
import com.estoque.service.form.ProductionForm;

/** Ordens de produção: Rascunho → Confirmada (entra na previsão) → Concluída (entra no estoque). */
@Service
@Transactional
public class ManufacturingService {

    private final ManufacturingOrderRepository orders;
    private final ProductRepository products;
    private final StockMoveRepository moves;
    private final LocationService locationService;
    private final StockService stockService;
    private final SequenceService sequences;

    public ManufacturingService(ManufacturingOrderRepository orders, ProductRepository products, StockMoveRepository moves,
                                LocationService locationService, StockService stockService, SequenceService sequences) {
        this.orders = orders;
        this.products = products;
        this.moves = moves;
        this.locationService = locationService;
        this.stockService = stockService;
        this.sequences = sequences;
    }

    @Transactional(readOnly = true)
    public ManufacturingOrder get(Long id) {
        return orders.findById(id).orElseThrow(() -> BusinessException.notFound("Ordem de produção"));
    }

    @Transactional(readOnly = true)
    public ProductionForm newForm() {
        var form = new ProductionForm();
        form.setLocationId(locationService.defaultStock().getId());
        return form;
    }

    public ManufacturingOrder save(ProductionForm form) {
        ManufacturingOrder order;
        if (form.getId() == null) {
            order = new ManufacturingOrder();
            order.setReference(sequences.nextProduction());
        } else {
            order = get(form.getId());
            if (order.getState() != ProductionState.DRAFT) {
                throw new BusinessException("Apenas ordens em rascunho podem ser alteradas.");
            }
        }
        order.setProduct(products.findById(form.getProductId()).orElseThrow(() -> BusinessException.notFound("Produto")));
        var location = locationService.get(form.getLocationId());
        if (!location.isInternal()) {
            throw new BusinessException("O destino da produção deve ser um local interno.");
        }
        order.setLocation(location);
        order.setQuantity(form.getQuantity());
        order.setScheduledDate(form.getScheduledDate());
        order.setOrigin(form.getOrigin());
        return orders.save(order);
    }

    /** Cria e confirma uma ordem (usado pela reposição). */
    public ManufacturingOrder createConfirmed(Product product, BigDecimal quantity, Location location, String origin) {
        var order = new ManufacturingOrder();
        order.setReference(sequences.nextProduction());
        order.setProduct(product);
        order.setQuantity(quantity);
        order.setLocation(location);
        order.setScheduledDate(LocalDate.now().plusDays(product.getLeadTimeDays()));
        order.setOrigin(origin);
        orders.save(order);
        confirm(order);
        return order;
    }

    public void confirm(Long id) {
        confirm(get(id));
    }

    private void confirm(ManufacturingOrder order) {
        if (order.getState() != ProductionState.DRAFT) {
            throw new BusinessException("Apenas ordens em rascunho podem ser confirmadas.");
        }
        var move = new StockMove(order.getProduct(), order.getQuantity(), locationService.virtual(LocationUsage.PRODUCTION),
                order.getLocation(), order.getReference());
        move.setState(MoveState.CONFIRMED);
        move.setDate(order.getScheduledDate().atTime(8, 0));
        move.setManufacturingOrder(order);
        moves.save(move);
        order.setState(ProductionState.CONFIRMED);
    }

    /** Conclui a produção, adicionando o produto ao estoque. */
    public void produce(Long id) {
        var order = get(id);
        if (order.getState() != ProductionState.CONFIRMED) {
            throw new BusinessException("Confirme a ordem antes de produzir.");
        }
        moves.findByManufacturingOrderId(id).stream()
                .filter(m -> m.getState() == MoveState.CONFIRMED)
                .forEach(stockService::applyMove);
        order.setState(ProductionState.DONE);
        order.setDoneDate(LocalDateTime.now());
    }

    public void cancel(Long id) {
        var order = get(id);
        if (order.getState() == ProductionState.DONE || order.getState() == ProductionState.CANCELLED) {
            throw new BusinessException("Esta ordem não pode mais ser cancelada.");
        }
        moves.findByManufacturingOrderId(id).forEach(m -> m.setState(MoveState.CANCELLED));
        order.setState(ProductionState.CANCELLED);
    }
}
