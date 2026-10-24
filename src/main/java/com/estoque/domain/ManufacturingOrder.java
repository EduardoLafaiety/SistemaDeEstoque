package com.estoque.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/** Ordem de produção: ao ser concluída, adiciona o produto acabado ao local de destino. */
@Entity
public class ManufacturingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String reference;

    @ManyToOne(optional = false)
    private Product product;

    @Column(nullable = false, precision = 16, scale = 3)
    private BigDecimal quantity;

    @ManyToOne(optional = false)
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductionState state = ProductionState.DRAFT;

    @Column(nullable = false)
    private LocalDate scheduledDate = LocalDate.now();

    private LocalDateTime doneDate;

    @Column(length = 120)
    private String origin;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public ProductionState getState() { return state; }
    public void setState(ProductionState state) { this.state = state; }
    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }
    public LocalDateTime getDoneDate() { return doneDate; }
    public void setDoneDate(LocalDateTime doneDate) { this.doneDate = doneDate; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
