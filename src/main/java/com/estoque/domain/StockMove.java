package com.estoque.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * Movimento de um produto entre dois locais. Movimentos confirmados (ainda não concluídos)
 * compõem a previsão de estoque; movimentos concluídos já foram aplicados aos saldos.
 */
@Entity
public class StockMove {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Picking picking;

    @ManyToOne(fetch = FetchType.LAZY)
    private ManufacturingOrder manufacturingOrder;

    @ManyToOne(optional = false)
    private Product product;

    @ManyToOne(optional = false)
    private Location sourceLocation;

    @ManyToOne(optional = false)
    private Location destLocation;

    @Column(nullable = false, precision = 16, scale = 3)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MoveState state = MoveState.DRAFT;

    @Column(length = 60)
    private String reference;

    @Column(name = "move_date", nullable = false)
    private LocalDateTime date = LocalDateTime.now();

    public StockMove() {
    }

    public StockMove(Product product, BigDecimal quantity, Location source, Location dest, String reference) {
        this.product = product;
        this.quantity = quantity;
        this.sourceLocation = source;
        this.destLocation = dest;
        this.reference = reference;
    }

    public Long getId() { return id; }
    public Picking getPicking() { return picking; }
    public void setPicking(Picking picking) { this.picking = picking; }
    public ManufacturingOrder getManufacturingOrder() { return manufacturingOrder; }
    public void setManufacturingOrder(ManufacturingOrder manufacturingOrder) { this.manufacturingOrder = manufacturingOrder; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Location getSourceLocation() { return sourceLocation; }
    public void setSourceLocation(Location sourceLocation) { this.sourceLocation = sourceLocation; }
    public Location getDestLocation() { return destLocation; }
    public void setDestLocation(Location destLocation) { this.destLocation = destLocation; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public MoveState getState() { return state; }
    public void setState(MoveState state) { this.state = state; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
}
