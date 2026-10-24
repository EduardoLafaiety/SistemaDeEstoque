package com.estoque.domain;

public enum Role {
    ADMIN("Administrador"),
    USER("Usuário");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
