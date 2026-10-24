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

@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;

    @Column(nullable = false, length = 20)
    private String uom = "Un";

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal cost = BigDecimal.ZERO;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal salePrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductRoute route = ProductRoute.BUY;

    /** Fornecedor padrão usado pela reposição de produtos com rota "Comprar". */
    @ManyToOne(fetch = FetchType.LAZY)
    private Partner supplier;

    private int leadTimeDays;

    @Column(length = 60)
    private String barcode;

    @Column(columnDefinition = "text")
    private String description;

    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Nome no estilo "[SKU] Nome". */
    public String getDisplayName() {
        return sku == null || sku.isBlank() ? name : "[" + sku + "] " + name;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getUom() { return uom; }
    public void setUom(String uom) { this.uom = uom; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public ProductRoute getRoute() { return route; }
    public void setRoute(ProductRoute route) { this.route = route; }
    public Partner getSupplier() { return supplier; }
    public void setSupplier(Partner supplier) { this.supplier = supplier; }
    public int getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(int leadTimeDays) { this.leadTimeDays = leadTimeDays; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
