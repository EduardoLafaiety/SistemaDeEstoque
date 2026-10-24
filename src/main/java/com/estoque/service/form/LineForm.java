package com.estoque.service.form;

import java.math.BigDecimal;

/** Linha de produto em operações e pedidos (o preço é usado apenas em compras). */
public class LineForm {

    private Long productId;
    private BigDecimal quantity;
    private BigDecimal unitPrice;

    public LineForm() {
    }

    public LineForm(Long productId, BigDecimal quantity, BigDecimal unitPrice) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    /** Linhas totalmente vazias (adicionadas e não preenchidas) são ignoradas. */
    public boolean isBlank() {
        return productId == null && quantity == null;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}
