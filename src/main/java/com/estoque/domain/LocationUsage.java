package com.estoque.domain;

public enum LocationUsage {
    VIEW("Visão"),
    INTERNAL("Interno"),
    SUPPLIER("Fornecedor"),
    CUSTOMER("Cliente"),
    INVENTORY("Ajuste de inventário"),
    PRODUCTION("Produção");

    private final String label;

    LocationUsage(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
