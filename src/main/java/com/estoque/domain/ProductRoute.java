package com.estoque.domain;

public enum ProductRoute {
    BUY("Comprar"),
    MANUFACTURE("Fabricar");

    private final String label;

    ProductRoute(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
