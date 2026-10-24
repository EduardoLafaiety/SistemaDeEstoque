package com.estoque.domain;

public enum PickingState {
    DRAFT("Rascunho"),
    READY("Pronto"),
    DONE("Concluído"),
    CANCELLED("Cancelado");

    private final String label;

    PickingState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
