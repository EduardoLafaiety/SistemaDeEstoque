package com.estoque.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Regra de reposição (mín./máx.): quando a previsão do produto no local fica abaixo do mínimo,
 * sugere-se pedir o suficiente para voltar ao máximo.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "location_id"}))
public class ReorderRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Product product;

    @ManyToOne(optional = false)
    private Location location;

    @Column(nullable = false, precision = 16, scale = 3)
    private BigDecimal minQty = BigDecimal.ZERO;

    @Column(nullable = false, precision = 16, scale = 3)
    private BigDecimal maxQty = BigDecimal.ZERO;

    @Column(nullable = false, precision = 16, scale = 3)
    private BigDecimal multipleQty = BigDecimal.ONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 10)
    private ReorderTrigger trigger = ReorderTrigger.MANUAL;

    private LocalDate snoozedUntil;

    private boolean active = true;

    public boolean isSnoozed() {
        return snoozedUntil != null && snoozedUntil.isAfter(LocalDate.now());
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public BigDecimal getMinQty() { return minQty; }
    public void setMinQty(BigDecimal minQty) { this.minQty = minQty; }
    public BigDecimal getMaxQty() { return maxQty; }
    public void setMaxQty(BigDecimal maxQty) { this.maxQty = maxQty; }
    public BigDecimal getMultipleQty() { return multipleQty; }
    public void setMultipleQty(BigDecimal multipleQty) { this.multipleQty = multipleQty; }
    public ReorderTrigger getTrigger() { return trigger; }
    public void setTrigger(ReorderTrigger trigger) { this.trigger = trigger; }
    public LocalDate getSnoozedUntil() { return snoozedUntil; }
    public void setSnoozedUntil(LocalDate snoozedUntil) { this.snoozedUntil = snoozedUntil; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
