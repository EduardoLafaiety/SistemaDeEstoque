package com.estoque.domain;

public enum ProductionState {
    DRAFT("Rascunho"),
    CONFIRMED("Confirmada"),
    DONE("Concluída"),
    CANCELLED("Cancelada");

    private final String label;

    ProductionState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
