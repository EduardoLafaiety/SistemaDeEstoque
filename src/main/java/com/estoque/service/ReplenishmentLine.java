package com.estoque.service;

import java.math.BigDecimal;

import com.estoque.domain.ReorderRule;

/** Linha da tela de reposição: a regra com saldo, previsão e quantidade sugerida. */
public class ReplenishmentLine {

    private final ReorderRule rule;
    private final BigDecimal onHand;
    private final BigDecimal forecast;
    private final BigDecimal inProgress;
    private final BigDecimal toOrder;
    private final BigDecimal toMax;

    public ReplenishmentLine(ReorderRule rule, BigDecimal onHand, BigDecimal forecast, BigDecimal inProgress) {
        this.rule = rule;
        this.onHand = onHand;
        this.forecast = forecast;
        this.inProgress = inProgress;
        BigDecimal virtual = forecast.add(inProgress);
        this.toOrder = ReplenishmentService.quantityToOrder(virtual, rule.getMinQty(), rule.getMaxQty(), rule.getMultipleQty(), false);
        this.toMax = ReplenishmentService.quantityToOrder(virtual, rule.getMinQty(), rule.getMaxQty(), rule.getMultipleQty(), true);
    }

    public boolean isNeedsOrder() {
        return toOrder.signum() > 0;
    }

    public ReorderRule getRule() { return rule; }
    public BigDecimal getOnHand() { return onHand; }
    public BigDecimal getForecast() { return forecast; }
    public BigDecimal getInProgress() { return inProgress; }
    public BigDecimal getToOrder() { return toOrder; }
    public BigDecimal getToMax() { return toMax; }
}
