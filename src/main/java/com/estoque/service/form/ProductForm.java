package com.estoque.service.form;

import java.math.BigDecimal;

import com.estoque.domain.Product;
import com.estoque.domain.ProductRoute;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProductForm {

    private Long id;

    @Size(max = 50)
    private String sku;

    @NotBlank(message = "Informe o nome do produto")
    @Size(max = 200)
    private String name;

    private Long categoryId;

    @NotBlank
    @Size(max = 20)
    private String uom = "Un";

    @NotNull
    @DecimalMin(value = "0.00", message = "O custo não pode ser negativo")
    private BigDecimal cost = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0.00", message = "O preço não pode ser negativo")
    private BigDecimal salePrice = BigDecimal.ZERO;

    @NotNull
    private ProductRoute route = ProductRoute.BUY;

    private Long supplierId;

    @Min(value = 0, message = "O prazo não pode ser negativo")
    private int leadTimeDays;

    @Size(max = 60)
    private String barcode;

    private String description;

    private boolean active = true;

    public static ProductForm from(Product p) {
        var f = new ProductForm();
        f.id = p.getId();
        f.sku = p.getSku();
        f.name = p.getName();
        f.categoryId = p.getCategory() == null ? null : p.getCategory().getId();
        f.uom = p.getUom();
        f.cost = p.getCost();
        f.salePrice = p.getSalePrice();
        f.route = p.getRoute();
        f.supplierId = p.getSupplier() == null ? null : p.getSupplier().getId();
        f.leadTimeDays = p.getLeadTimeDays();
        f.barcode = p.getBarcode();
        f.description = p.getDescription();
        f.active = p.isActive();
        return f;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getUom() { return uom; }
    public void setUom(String uom) { this.uom = uom; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public ProductRoute getRoute() { return route; }
    public void setRoute(ProductRoute route) { this.route = route; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public int getLeadTimeDays() { return leadTimeDays; }
    public void setLeadTimeDays(int leadTimeDays) { this.leadTimeDays = leadTimeDays; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
