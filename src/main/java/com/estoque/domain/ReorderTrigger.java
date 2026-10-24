package com.estoque.domain;

public enum ReorderTrigger {
    AUTO("Automático"),
    MANUAL("Manual");

    private final String label;

    ReorderTrigger(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
