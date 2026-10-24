package com.estoque.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

/** Operação de estoque: recebimento, entrega ou transferência interna. */
@Entity
public class Picking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "picking_type", nullable = false, length = 20)
    private PickingType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PickingState state = PickingState.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    private Partner partner;

    @ManyToOne(optional = false)
    private Location sourceLocation;

    @ManyToOne(optional = false)
    private Location destLocation;

    @Column(nullable = false)
    private LocalDateTime scheduledDate;

    private LocalDateTime doneDate;

    @Column(length = 120)
    private String origin;

    @Column(columnDefinition = "text")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    private PurchaseOrder purchaseOrder;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(length = 50)
    private String createdBy;

    @OneToMany(mappedBy = "picking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<StockMove> moves = new ArrayList<>();

    public void addMove(StockMove move) {
        move.setPicking(this);
        moves.add(move);
    }

    public boolean isEditable() {
        return state == PickingState.DRAFT || state == PickingState.READY;
    }

    public boolean isLate() {
        return state == PickingState.READY && scheduledDate.isBefore(LocalDateTime.now());
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public PickingType getType() { return type; }
    public void setType(PickingType type) { this.type = type; }
    public PickingState getState() { return state; }
    public void setState(PickingState state) { this.state = state; }
    public Partner getPartner() { return partner; }
    public void setPartner(Partner partner) { this.partner = partner; }
    public Location getSourceLocation() { return sourceLocation; }
    public void setSourceLocation(Location sourceLocation) { this.sourceLocation = sourceLocation; }
    public Location getDestLocation() { return destLocation; }
    public void setDestLocation(Location destLocation) { this.destLocation = destLocation; }
    public LocalDateTime getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDateTime scheduledDate) { this.scheduledDate = scheduledDate; }
    public LocalDateTime getDoneDate() { return doneDate; }
    public void setDoneDate(LocalDateTime doneDate) { this.doneDate = doneDate; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public List<StockMove> getMoves() { return moves; }
}
