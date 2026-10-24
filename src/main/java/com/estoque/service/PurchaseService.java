package com.estoque.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.estoque.domain.Location;
import com.estoque.domain.Picking;
import com.estoque.domain.PickingState;
import com.estoque.domain.Product;
import com.estoque.domain.PurchaseOrder;
import com.estoque.domain.PurchaseOrderLine;
import com.estoque.domain.PurchaseState;
import com.estoque.repository.PartnerRepository;
import com.estoque.repository.PickingRepository;
import com.estoque.repository.ProductRepository;
import com.estoque.repository.PurchaseOrderRepository;
import com.estoque.service.form.LineForm;
import com.estoque.service.form.PurchaseForm;

/** Solicitação de cotação → Pedido de compra (gera recebimento) → Recebido. */
@Service
@Transactional
public class PurchaseService {

    private final PurchaseOrderRepository orders;
    private final PartnerRepository partners;
    private final ProductRepository products;
    private final PickingRepository pickings;
    private final LocationService locationService;
    private final PickingService pickingService;
    private final SequenceService sequences;

    public PurchaseService(PurchaseOrderRepository orders, PartnerRepository partners, ProductRepository products,
                           PickingRepository pickings, LocationService locationService, PickingService pickingService,
                           SequenceService sequences) {
        this.orders = orders;
        this.partners = partners;
        this.products = products;
        this.pickings = pickings;
        this.locationService = locationService;
        this.pickingService = pickingService;
        this.sequences = sequences;
    }

    @Transactional(readOnly = true)
    public PurchaseOrder get(Long id) {
        return orders.findById(id).orElseThrow(() -> BusinessException.notFound("Pedido de compra"));
    }

    @Transactional(readOnly = true)
    public PurchaseForm newForm() {
        var form = new PurchaseForm();
        form.setDestLocationId(locationService.defaultStock().getId());
        form.setExpectedDate(LocalDate.now().plusDays(7));
        form.getLines().add(new LineForm());
        return form;
    }

    public PurchaseOrder save(PurchaseForm form) {
        PurchaseOrder order;
        if (form.getId() == null) {
            order = new PurchaseOrder();
            order.setReference(sequences.nextPurchase());
        } else {
            order = get(form.getId());
            if (order.getState() != PurchaseState.DRAFT) {
                throw new BusinessException("Apenas solicitações de cotação podem ser alteradas.");
            }
        }
        order.setPartner(partners.findById(form.getPartnerId()).orElseThrow(() -> BusinessException.notFound("Fornecedor")));
        var dest = locationService.get(form.getDestLocationId());
        if (!dest.isInternal()) {
            throw new BusinessException("O local de entrega deve ser um local interno.");
        }
        order.setDestLocation(dest);
        order.setExpectedDate(form.getExpectedDate());
        order.setOrigin(form.getOrigin());
        order.setNotes(form.getNotes());

        order.getLines().clear();
        for (LineForm line : form.getLines()) {
            if (line.isBlank()) {
                continue;
            }
            if (line.getProductId() == null || line.getQuantity() == null || line.getQuantity().signum() <= 0) {
                throw new BusinessException("Cada linha precisa de um produto e de uma quantidade maior que zero.");
            }
            var product = products.findById(line.getProductId()).orElseThrow(() -> BusinessException.notFound("Produto"));
            BigDecimal price = line.getUnitPrice() == null ? product.getCost() : line.getUnitPrice();
            if (price.signum() < 0) {
                throw new BusinessException("O preço unitário não pode ser negativo.");
            }
            order.addLine(new PurchaseOrderLine(product, line.getQuantity(), price));
        }
        if (order.getLines().isEmpty()) {
            throw new BusinessException("Adicione ao menos um produto.");
        }
        return orders.save(order);
    }

    /**
     * Adiciona a quantidade à solicitação de cotação aberta do fornecedor para o local
     * (ou cria uma nova). Usado pela reposição.
     */
    public PurchaseOrder addToDraft(Product product, BigDecimal quantity, Location dest, String origin) {
        if (product.getSupplier() == null) {
            throw new BusinessException("Defina um fornecedor para o produto " + product.getDisplayName() + ".");
        }
        var order = orders.findFirstByPartnerAndDestLocationAndStateOrderByIdDesc(product.getSupplier(), dest, PurchaseState.DRAFT)
                .orElseGet(() -> {
                    var created = new PurchaseOrder();
                    created.setReference(sequences.nextPurchase());
                    created.setPartner(product.getSupplier());
                    created.setDestLocation(dest);
                    created.setExpectedDate(LocalDate.now().plusDays(product.getLeadTimeDays()));
                    created.setOrigin(origin);
                    return created;
                });
        order.getLines().stream()
                .filter(l -> l.getProduct().getId().equals(product.getId()))
                .findFirst()
                .ifPresentOrElse(
                        l -> l.setQuantity(l.getQuantity().add(quantity)),
                        () -> order.addLine(new PurchaseOrderLine(product, quantity, product.getCost())));
        return orders.save(order);
    }

    /** Confirma a cotação e cria o recebimento correspondente. */
    public Picking confirm(Long id) {
        var order = get(id);
        if (order.getState() != PurchaseState.DRAFT) {
            throw new BusinessException("Apenas solicitações de cotação podem ser confirmadas.");
        }
        if (order.getLines().isEmpty()) {
            throw new BusinessException("O pedido não tem produtos.");
        }
        order.setState(PurchaseState.CONFIRMED);
        return pickingService.createReceipt(order);
    }

    public void cancel(Long id) {
        var order = get(id);
        if (order.getState() == PurchaseState.DONE || order.getState() == PurchaseState.CANCELLED) {
            throw new BusinessException("Este pedido não pode mais ser cancelado.");
        }
        for (var picking : pickings.findByPurchaseOrderId(id)) {
            if (picking.getState() == PickingState.DONE) {
                throw new BusinessException("O recebimento " + picking.getReference() + " já foi concluído.");
            }
            if (picking.isEditable()) {
                pickingService.cancel(picking.getId());
            }
        }
        order.setState(PurchaseState.CANCELLED);
    }

    public void delete(Long id) {
        var order = get(id);
        if (order.getState() != PurchaseState.DRAFT && order.getState() != PurchaseState.CANCELLED) {
            throw new BusinessException("Apenas cotações em rascunho ou canceladas podem ser excluídas.");
        }
        pickings.findByPurchaseOrderId(id).forEach(pickings::delete);
        orders.delete(order);
    }
}
