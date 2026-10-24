package com.estoque.service.form;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.estoque.domain.ManufacturingOrder;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProductionForm {

    private Long id;

    @NotNull(message = "Informe o produto")
    private Long productId;

    @NotNull(message = "Informe a quantidade")
    @DecimalMin(value = "0.001", message = "A quantidade deve ser maior que zero")
    private BigDecimal quantity;

    @NotNull(message = "Informe o local de destino")
    private Long locationId;

    @NotNull(message = "Informe a data programada")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate scheduledDate = LocalDate.now();

    @Size(max = 120)
    private String origin;

    public static ProductionForm from(ManufacturingOrder o) {
        var f = new ProductionForm();
        f.id = o.getId();
        f.productId = o.getProduct().getId();
        f.quantity = o.getQuantity();
        f.locationId = o.getLocation().getId();
        f.scheduledDate = o.getScheduledDate();
        f.origin = o.getOrigin();
        return f;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
}
