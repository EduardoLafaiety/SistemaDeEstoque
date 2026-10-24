package com.estoque.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Saldo físico de um produto em um local interno. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "location_id"}))
public class StockQuant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Product product;

    @ManyToOne(optional = false)
    private Location location;

    @Column(nullable = false, precision = 16, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;

    public StockQuant() {
    }

    public StockQuant(Product product, Location location) {
        this.product = product;
        this.location = location;
    }

    public BigDecimal getValue() {
        return quantity.multiply(product.getCost());
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public Location getLocation() { return location; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
}
