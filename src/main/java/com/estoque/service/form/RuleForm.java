package com.estoque.service.form;

import java.math.BigDecimal;

import com.estoque.domain.ReorderRule;
import com.estoque.domain.ReorderTrigger;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class RuleForm {

    private Long id;

    @NotNull(message = "Informe o produto")
    private Long productId;

    @NotNull(message = "Informe o local")
    private Long locationId;

    @NotNull
    @DecimalMin(value = "0", message = "O mínimo não pode ser negativo")
    private BigDecimal minQty = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0", message = "O máximo não pode ser negativo")
    private BigDecimal maxQty = BigDecimal.ZERO;

    @NotNull
    @DecimalMin(value = "0", message = "O múltiplo não pode ser negativo")
    private BigDecimal multipleQty = BigDecimal.ONE;

    @NotNull
    private ReorderTrigger trigger = ReorderTrigger.MANUAL;

    public static RuleForm from(ReorderRule r) {
        var f = new RuleForm();
        f.id = r.getId();
        f.productId = r.getProduct().getId();
        f.locationId = r.getLocation().getId();
        f.minQty = r.getMinQty();
        f.maxQty = r.getMaxQty();
        f.multipleQty = r.getMultipleQty();
        f.trigger = r.getTrigger();
        return f;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public BigDecimal getMinQty() { return minQty; }
    public void setMinQty(BigDecimal minQty) { this.minQty = minQty; }
    public BigDecimal getMaxQty() { return maxQty; }
    public void setMaxQty(BigDecimal maxQty) { this.maxQty = maxQty; }
    public BigDecimal getMultipleQty() { return multipleQty; }
    public void setMultipleQty(BigDecimal multipleQty) { this.multipleQty = multipleQty; }
    public ReorderTrigger getTrigger() { return trigger; }
    public void setTrigger(ReorderTrigger trigger) { this.trigger = trigger; }
}
