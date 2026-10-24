package com.estoque.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.estoque.domain.Location;
import com.estoque.domain.LocationUsage;
import com.estoque.domain.ProductRoute;
import com.estoque.domain.ReorderRule;
import com.estoque.domain.ReorderTrigger;
import com.estoque.domain.Warehouse;
import com.estoque.repository.LocationRepository;
import com.estoque.repository.ManufacturingOrderRepository;
import com.estoque.repository.ProductRepository;
import com.estoque.repository.PurchaseOrderRepository;
import com.estoque.repository.ReorderRuleRepository;
import com.estoque.repository.StockMoveRepository;
import com.estoque.repository.WarehouseRepository;
import com.estoque.service.form.RuleForm;

/**
 * Reposição por regras mín./máx. no estilo do Odoo: calcula a previsão de cada regra,
 * sugere quanto pedir e gera cotações de compra ou ordens de produção.
 */
@Service
@Transactional
public class ReplenishmentService {

    public static final String ORIGIN = "Reposição";
    private static final Logger log = LoggerFactory.getLogger(ReplenishmentService.class);

    private final ReorderRuleRepository rules;
    private final ProductRepository products;
    private final LocationRepository locations;
    private final WarehouseRepository warehouses;
    private final StockMoveRepository moves;
    private final PurchaseOrderRepository purchaseOrders;
    private final ManufacturingOrderRepository manufacturingOrders;
    private final StockService stockService;
    private final PurchaseService purchaseService;
    private final ManufacturingService manufacturingService;

    public ReplenishmentService(ReorderRuleRepository rules, ProductRepository products, LocationRepository locations,
                                WarehouseRepository warehouses, StockMoveRepository moves,
                                PurchaseOrderRepository purchaseOrders, ManufacturingOrderRepository manufacturingOrders,
                                StockService stockService, PurchaseService purchaseService,
                                ManufacturingService manufacturingService) {
        this.rules = rules;
        this.products = products;
        this.locations = locations;
        this.warehouses = warehouses;
        this.moves = moves;
        this.purchaseOrders = purchaseOrders;
        this.manufacturingOrders = manufacturingOrders;
        this.stockService = stockService;
        this.purchaseService = purchaseService;
        this.manufacturingService = manufacturingService;
    }

    /**
     * Quantidade a pedir. Normalmente só há pedido quando a previsão fica abaixo do mínimo;
     * com {@code toMax} pede-se sempre o necessário para chegar ao máximo. Arredonda para cima no múltiplo.
     */
    public static BigDecimal quantityToOrder(BigDecimal forecast, BigDecimal min, BigDecimal max, BigDecimal multiple,
                                             boolean toMax) {
        if (!toMax && forecast.compareTo(min) >= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal qty = max.max(min).subtract(forecast);
        if (qty.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (multiple != null && multiple.signum() > 0) {
            qty = qty.divide(multiple, 0, RoundingMode.CEILING).multiply(multiple);
        }
        return qty;
    }

    @Transactional(readOnly = true)
    public ReplenishmentLine line(ReorderRule rule) {
        var product = rule.getProduct();
        var location = rule.getLocation();
        String path = location.getFullName();
        BigDecimal inProgress = product.getRoute() == ProductRoute.BUY
                ? purchaseOrders.sumDraftQuantity(product.getId(), path)
                : manufacturingOrders.sumDraftQuantity(product.getId(), path);
        return new ReplenishmentLine(rule, stockService.onHand(product, location),
                stockService.forecast(product, location), inProgress);
    }

    /** Monta a tela de reposição: linhas filtradas e contadores da barra lateral. */
    @Transactional(readOnly = true)
    public ReplenishmentView view(ReplenishmentFilter filter) {
        String term = filter.term() == null ? "" : filter.term().trim().toLowerCase();
        Location selected = filter.locationId() == null ? null : locations.findById(filter.locationId()).orElse(null);

        Predicate<ReorderRule> base = r -> r.isSnoozed() == filter.snoozed()
                && (term.isEmpty() || r.getProduct().getDisplayName().toLowerCase().contains(term));
        List<ReorderRule> candidates = rules.findAllActive().stream().filter(base).toList();

        List<ReplenishmentLine> computed = candidates.stream().map(this::line)
                .filter(l -> !filter.toOrderOnly() || l.isNeedsOrder())
                .toList();

        // Contadores consideram os demais filtros, exceto o próprio grupo.
        List<ReplenishmentLine> byTrigger = computed.stream()
                .filter(l -> selected == null || l.getRule().getLocation().isChildOf(selected)).toList();
        List<ReplenishmentLine> byLocation = computed.stream()
                .filter(l -> filter.trigger() == null || l.getRule().getTrigger() == filter.trigger()).toList();

        var locationCounts = new ArrayList<ReplenishmentView.LocationCount>();
        for (Location location : locations.findByActiveTrueOrderByFullName()) {
            if (location.getUsage() != LocationUsage.VIEW && location.getUsage() != LocationUsage.INTERNAL) {
                continue;
            }
            long count = byLocation.stream().filter(l -> l.getRule().getLocation().isChildOf(location)).count();
            if (count > 0) {
                locationCounts.add(new ReplenishmentView.LocationCount(location, count));
            }
        }
        long auto = byTrigger.stream().filter(l -> l.getRule().getTrigger() == ReorderTrigger.AUTO).count();

        List<ReplenishmentLine> lines = byTrigger.stream()
                .filter(l -> filter.trigger() == null || l.getRule().getTrigger() == filter.trigger())
                .sorted(Comparator.comparing((ReplenishmentLine l) -> !l.isNeedsOrder())
                        .thenComparing(l -> l.getRule().getProduct().getName(), String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new ReplenishmentView(lines, locationCounts, byLocation.size(), byTrigger.size(), auto, byTrigger.size() - auto);
    }

    @Transactional(readOnly = true)
    public ReorderRule get(Long id) {
        return rules.findById(id).orElseThrow(() -> BusinessException.notFound("Regra de reposição"));
    }

    public ReorderRule save(RuleForm form) {
        if (form.getMaxQty().compareTo(form.getMinQty()) < 0) {
            throw new BusinessException("O máximo deve ser maior ou igual ao mínimo.");
        }
        var existing = rules.findByProductIdAndLocationId(form.getProductId(), form.getLocationId());
        if (existing.isPresent() && !existing.get().getId().equals(form.getId())) {
            throw new BusinessException("Já existe uma regra para este produto neste local.");
        }
        var rule = form.getId() == null ? new ReorderRule() : get(form.getId());
        rule.setProduct(products.findById(form.getProductId()).orElseThrow(() -> BusinessException.notFound("Produto")));
        var location = locations.findById(form.getLocationId()).orElseThrow(() -> BusinessException.notFound("Local"));
        if (!location.isInternal()) {
            throw new BusinessException("Regras de reposição só podem ser criadas em locais internos.");
        }
        rule.setLocation(location);
        rule.setMinQty(form.getMinQty());
        rule.setMaxQty(form.getMaxQty());
        rule.setMultipleQty(form.getMultipleQty());
        rule.setTrigger(form.getTrigger());
        return rules.save(rule);
    }

    /** Edição rápida de mín./máx. direto na grade. */
    public void updateQuantities(Long id, BigDecimal min, BigDecimal max) {
        if (min == null || max == null || min.signum() < 0 || max.signum() < 0) {
            throw new BusinessException("Mínimo e máximo devem ser zero ou positivos.");
        }
        if (max.compareTo(min) < 0) {
            throw new BusinessException("O máximo deve ser maior ou igual ao mínimo.");
        }
        var rule = get(id);
        rule.setMinQty(min);
        rule.setMaxQty(max);
    }

    /** "Pedir uma vez" (ou "Pedir até o máximo"): gera cotação/ordem para cada regra selecionada. */
    public List<String> order(List<Long> ids, boolean toMax) {
        var references = new ArrayList<String>();
        for (ReorderRule rule : rules.findAllById(ids)) {
            var line = line(rule);
            BigDecimal qty = toMax ? line.getToMax() : line.getToOrder();
            if (qty.signum() > 0) {
                references.add(orderQuantity(rule, qty));
            }
        }
        if (references.isEmpty()) {
            throw new BusinessException("Nada a pedir nas linhas selecionadas.");
        }
        return references.stream().distinct().toList();
    }

    private String orderQuantity(ReorderRule rule, BigDecimal qty) {
        var product = rule.getProduct();
        if (product.getRoute() == ProductRoute.MANUFACTURE) {
            return manufacturingService.createConfirmed(product, qty, rule.getLocation(), ORIGIN).getReference();
        }
        return purchaseService.addToDraft(product, qty, rule.getLocation(), ORIGIN).getReference();
    }

    public void snooze(List<Long> ids, LocalDate until) {
        if (until == null || !until.isAfter(LocalDate.now())) {
            throw new BusinessException("Escolha uma data futura para adiar.");
        }
        rules.findAllById(ids).forEach(r -> r.setSnoozedUntil(until));
    }

    public void unsnooze(List<Long> ids) {
        rules.findAllById(ids).forEach(r -> r.setSnoozedUntil(null));
    }

    public void setTrigger(List<Long> ids, ReorderTrigger trigger) {
        rules.findAllById(ids).forEach(r -> r.setTrigger(trigger));
    }

    public void delete(List<Long> ids) {
        rules.deleteAllById(ids);
    }

    /**
     * Cria regras manuais (mín. 0 / máx. 0) para produtos com previsão negativa no estoque
     * de cada armazém, para que apareçam na reposição mesmo sem regra cadastrada.
     */
    public int syncManualRules() {
        int created = 0;
        for (Warehouse warehouse : warehouses.findAllByOrderById()) {
            var stock = warehouse.getStockLocation();
            for (var product : moves.findProductsWithPendingOutgoing(stock.getFullName())) {
                if (!product.isActive() || rules.findByProductIdAndLocationId(product.getId(), stock.getId()).isPresent()) {
                    continue;
                }
                if (stockService.forecast(product, stock).signum() < 0) {
                    var rule = new ReorderRule();
                    rule.setProduct(product);
                    rule.setLocation(stock);
                    rules.save(rule);
                    created++;
                }
            }
        }
        return created;
    }

    /** Executa as regras automáticas que precisam de reposição (chamado pelo agendador). */
    public List<String> runAutomatic() {
        var references = new ArrayList<String>();
        for (ReorderRule rule : rules.findAllActive()) {
            if (rule.getTrigger() != ReorderTrigger.AUTO || rule.isSnoozed()) {
                continue;
            }
            var line = line(rule);
            if (!line.isNeedsOrder()) {
                continue;
            }
            var product = rule.getProduct();
            if (product.getRoute() == ProductRoute.BUY && product.getSupplier() == null) {
                log.warn("Reposição automática ignorada: {} não tem fornecedor", product.getDisplayName());
                continue;
            }
            references.add(orderQuantity(rule, line.getToOrder()));
        }
        return references.stream().distinct().toList();
    }
}
