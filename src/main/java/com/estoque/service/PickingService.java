package com.estoque.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.estoque.domain.LocationUsage;
import com.estoque.domain.MoveState;
import com.estoque.domain.Picking;
import com.estoque.domain.PickingState;
import com.estoque.domain.PickingType;
import com.estoque.domain.PurchaseOrder;
import com.estoque.domain.PurchaseState;
import com.estoque.domain.StockMove;
import com.estoque.repository.PartnerRepository;
import com.estoque.repository.PickingRepository;
import com.estoque.repository.ProductRepository;
import com.estoque.service.form.LineForm;
import com.estoque.service.form.PickingForm;

/** Recebimentos, entregas e transferências internas: Rascunho → Pronto → Concluído. */
@Service
@Transactional
public class PickingService {

    private final PickingRepository pickings;
    private final ProductRepository products;
    private final PartnerRepository partners;
    private final LocationService locationService;
    private final StockService stockService;
    private final SequenceService sequences;

    public PickingService(PickingRepository pickings, ProductRepository products, PartnerRepository partners,
                          LocationService locationService, StockService stockService, SequenceService sequences) {
        this.pickings = pickings;
        this.products = products;
        this.partners = partners;
        this.locationService = locationService;
        this.stockService = stockService;
        this.sequences = sequences;
    }

    @Transactional(readOnly = true)
    public Picking get(Long id) {
        return pickings.findById(id).orElseThrow(() -> BusinessException.notFound("Operação"));
    }

    /** Formulário de nova operação com os locais padrão do tipo. */
    @Transactional(readOnly = true)
    public PickingForm newForm(PickingType type) {
        var form = new PickingForm();
        form.setType(type);
        form.setScheduledDate(LocalDateTime.now().withSecond(0).withNano(0));
        var stock = locationService.defaultStock();
        switch (type) {
            case RECEIPT -> {
                form.setSourceLocationId(locationService.virtual(LocationUsage.SUPPLIER).getId());
                form.setDestLocationId(stock.getId());
            }
            case DELIVERY -> {
                form.setSourceLocationId(stock.getId());
                form.setDestLocationId(locationService.virtual(LocationUsage.CUSTOMER).getId());
            }
            case INTERNAL -> {
                form.setSourceLocationId(stock.getId());
                form.setDestLocationId(stock.getId());
            }
        }
        form.getLines().add(new LineForm());
        return form;
    }

    public Picking save(PickingForm form, String username) {
        Picking picking;
        if (form.getId() == null) {
            picking = new Picking();
            picking.setType(form.getType());
            picking.setReference(sequences.nextPicking(form.getType()));
            picking.setCreatedBy(username);
        } else {
            picking = get(form.getId());
            if (!picking.isEditable()) {
                throw new BusinessException("A operação " + picking.getReference() + " não pode mais ser alterada.");
            }
        }
        var source = locationService.get(form.getSourceLocationId());
        var dest = locationService.get(form.getDestLocationId());
        if (source.getId().equals(dest.getId())) {
            throw new BusinessException("Os locais de origem e destino devem ser diferentes.");
        }
        if (!source.isInternal() && !dest.isInternal()) {
            throw new BusinessException("A origem ou o destino precisa ser um local interno.");
        }
        picking.setSourceLocation(source);
        picking.setDestLocation(dest);
        picking.setPartner(form.getPartnerId() == null ? null
                : partners.findById(form.getPartnerId()).orElseThrow(() -> BusinessException.notFound("Parceiro")));
        picking.setScheduledDate(form.getScheduledDate());
        picking.setOrigin(form.getOrigin());
        picking.setNotes(form.getNotes());

        MoveState moveState = picking.getState() == PickingState.READY ? MoveState.CONFIRMED : MoveState.DRAFT;
        picking.getMoves().clear();
        for (LineForm line : form.getLines()) {
            if (line.isBlank()) {
                continue;
            }
            if (line.getProductId() == null || line.getQuantity() == null || line.getQuantity().signum() <= 0) {
                throw new BusinessException("Cada linha precisa de um produto e de uma quantidade maior que zero.");
            }
            var product = products.findById(line.getProductId()).orElseThrow(() -> BusinessException.notFound("Produto"));
            var move = new StockMove(product, line.getQuantity(), source, dest, picking.getReference());
            move.setState(moveState);
            picking.addMove(move);
        }
        if (picking.getMoves().isEmpty()) {
            throw new BusinessException("Adicione ao menos um produto.");
        }
        return pickings.save(picking);
    }

    /** Rascunho → Pronto: os movimentos passam a contar na previsão. */
    public void confirm(Long id) {
        var picking = get(id);
        requireState(picking, PickingState.DRAFT);
        picking.setState(PickingState.READY);
        picking.getMoves().forEach(m -> m.setState(MoveState.CONFIRMED));
    }

    /** Conclui a operação aplicando os movimentos aos saldos. */
    public void validate(Long id) {
        var picking = get(id);
        if (!picking.isEditable()) {
            throw new BusinessException("A operação " + picking.getReference() + " já está " + picking.getState().getLabel().toLowerCase() + ".");
        }
        for (StockMove move : picking.getMoves()) {
            move.setState(MoveState.CONFIRMED);
            stockService.applyMove(move);
        }
        picking.setState(PickingState.DONE);
        picking.setDoneDate(LocalDateTime.now());

        var order = picking.getPurchaseOrder();
        if (order != null && pickings.findByPurchaseOrderId(order.getId()).stream()
                .allMatch(p -> p.getState() == PickingState.DONE || p.getState() == PickingState.CANCELLED)) {
            order.setState(PurchaseState.DONE);
        }
    }

    public void cancel(Long id) {
        var picking = get(id);
        if (!picking.isEditable()) {
            throw new BusinessException("Apenas operações em rascunho ou prontas podem ser canceladas.");
        }
        picking.setState(PickingState.CANCELLED);
        picking.getMoves().forEach(m -> m.setState(MoveState.CANCELLED));
    }

    public void delete(Long id) {
        var picking = get(id);
        if (picking.getState() != PickingState.DRAFT && picking.getState() != PickingState.CANCELLED) {
            throw new BusinessException("Apenas operações em rascunho ou canceladas podem ser excluídas.");
        }
        pickings.delete(picking);
    }

    /** Cria um recebimento pronto (usado na confirmação de pedidos de compra). */
    Picking createReceipt(PurchaseOrder order) {
        var picking = new Picking();
        picking.setType(PickingType.RECEIPT);
        picking.setReference(sequences.nextPicking(PickingType.RECEIPT));
        picking.setState(PickingState.READY);
        picking.setPartner(order.getPartner());
        picking.setSourceLocation(locationService.virtual(LocationUsage.SUPPLIER));
        picking.setDestLocation(order.getDestLocation());
        picking.setScheduledDate(order.getExpectedDate() == null ? LocalDateTime.now() : order.getExpectedDate().atTime(8, 0));
        picking.setOrigin(order.getReference());
        picking.setPurchaseOrder(order);
        for (var line : order.getLines()) {
            var move = new StockMove(line.getProduct(), line.getQuantity(), picking.getSourceLocation(),
                    picking.getDestLocation(), picking.getReference());
            move.setState(MoveState.CONFIRMED);
            picking.addMove(move);
        }
        return pickings.save(picking);
    }

    private static void requireState(Picking picking, PickingState expected) {
        if (picking.getState() != expected) {
            throw new BusinessException("A operação " + picking.getReference() + " precisa estar em \"" + expected.getLabel() + "\".");
        }
    }
}
