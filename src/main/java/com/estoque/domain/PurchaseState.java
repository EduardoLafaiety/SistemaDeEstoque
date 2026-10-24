package com.estoque.domain;

public enum PurchaseState {
    DRAFT("Solicitação de cotação"),
    CONFIRMED("Pedido de compra"),
    DONE("Recebido"),
    CANCELLED("Cancelado");

    private final String label;

    PurchaseState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
