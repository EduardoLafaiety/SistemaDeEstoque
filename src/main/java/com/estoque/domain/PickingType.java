package com.estoque.domain;

public enum PickingType {
    RECEIPT("Recebimento", "Recebimentos", "IN"),
    DELIVERY("Entrega", "Entregas", "OUT"),
    INTERNAL("Transferência interna", "Transferências internas", "INT");

    private final String label;
    private final String pluralLabel;
    private final String prefix;

    PickingType(String label, String pluralLabel, String prefix) {
        this.label = label;
        this.pluralLabel = pluralLabel;
        this.prefix = prefix;
    }

    public String getLabel() {
        return label;
    }

    public String getPluralLabel() {
        return pluralLabel;
    }

    public String getPrefix() {
        return prefix;
    }
}
