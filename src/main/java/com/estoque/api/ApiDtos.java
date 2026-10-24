package com.estoque.api;

import java.math.BigDecimal;

import com.estoque.domain.Product;
import com.estoque.domain.StockQuant;
import com.estoque.service.ReplenishmentLine;

/** Representações JSON expostas pela API. */
public final class ApiDtos {

    private ApiDtos() {
    }

    public record ProductDto(Long id, String sku, String name, String category, String uom, BigDecimal cost,
                             BigDecimal salePrice, String route, String supplier, int leadTimeDays, boolean active,
                             BigDecimal onHand) {

        static ProductDto of(Product p, BigDecimal onHand) {
            return new ProductDto(p.getId(), p.getSku(), p.getName(),
                    p.getCategory() == null ? null : p.getCategory().getName(), p.getUom(), p.getCost(),
                    p.getSalePrice(), p.getRoute().name(), p.getSupplier() == null ? null : p.getSupplier().getName(),
                    p.getLeadTimeDays(), p.isActive(), onHand);
        }
    }

    public record StockDto(Long productId, String sku, String product, String location, BigDecimal quantity,
                           BigDecimal value) {

        static StockDto of(StockQuant q) {
            return new StockDto(q.getProduct().getId(), q.getProduct().getSku(), q.getProduct().getName(),
                    q.getLocation().getFullName(), q.getQuantity(), q.getValue());
        }
    }

    public record ReplenishmentDto(Long ruleId, Long productId, String product, String location, String route,
                                   String trigger, BigDecimal onHand, BigDecimal forecast, BigDecimal inProgress,
                                   BigDecimal minQty, BigDecimal maxQty, BigDecimal toOrder, boolean snoozed) {

        static ReplenishmentDto of(ReplenishmentLine l) {
            var r = l.getRule();
            return new ReplenishmentDto(r.getId(), r.getProduct().getId(), r.getProduct().getDisplayName(),
                    r.getLocation().getFullName(), r.getProduct().getRoute().name(), r.getTrigger().name(),
                    l.getOnHand(), l.getForecast(), l.getInProgress(), r.getMinQty(), r.getMaxQty(), l.getToOrder(),
                    r.isSnoozed());
        }
    }

    public record OrderResult(java.util.List<String> references) {
    }

    public record ErrorDto(String message) {
    }
}
