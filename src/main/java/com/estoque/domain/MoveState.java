package com.estoque.domain;

public enum MoveState {
    DRAFT("Rascunho"),
    CONFIRMED("Confirmado"),
    DONE("Concluído"),
    CANCELLED("Cancelado");

    private final String label;

    MoveState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
