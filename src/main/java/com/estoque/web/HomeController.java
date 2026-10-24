package com.estoque.web;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.estoque.domain.MoveState;
import com.estoque.domain.PickingState;
import com.estoque.domain.PickingType;
import com.estoque.domain.ProductionState;
import com.estoque.domain.PurchaseState;
import com.estoque.repository.ManufacturingOrderRepository;
import com.estoque.repository.PickingRepository;
import com.estoque.repository.ProductRepository;
import com.estoque.repository.PurchaseOrderRepository;
import com.estoque.repository.StockMoveRepository;
import com.estoque.repository.StockQuantRepository;
import com.estoque.service.ReplenishmentFilter;
import com.estoque.service.ReplenishmentService;

@Controller
public class HomeController {

    private final PickingRepository pickings;
    private final ProductRepository products;
    private final StockQuantRepository quants;
    private final StockMoveRepository moves;
    private final PurchaseOrderRepository purchases;
    private final ManufacturingOrderRepository productions;
    private final ReplenishmentService replenishment;

    public HomeController(PickingRepository pickings, ProductRepository products, StockQuantRepository quants,
                          StockMoveRepository moves, PurchaseOrderRepository purchases,
                          ManufacturingOrderRepository productions, ReplenishmentService replenishment) {
        this.pickings = pickings;
        this.products = products;
        this.quants = quants;
        this.moves = moves;
        this.purchases = purchases;
        this.productions = productions;
        this.replenishment = replenishment;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /** Visão geral: um cartão por tipo de operação e indicadores do estoque. */
    @GetMapping("/")
    public String overview(Model model) {
        var now = LocalDateTime.now();
        List<OperationCard> cards = Arrays.stream(PickingType.values())
                .map(type -> new OperationCard(type,
                        pickings.countByTypeAndState(type, PickingState.READY),
                        pickings.countByTypeAndStateAndScheduledDateBefore(type, PickingState.READY, now),
                        pickings.countByTypeAndState(type, PickingState.DRAFT)))
                .toList();
        long toReorder = replenishment.view(new ReplenishmentFilter(null, null, null, true, false)).getLines().size();

        model.addAttribute("cards", cards);
        model.addAttribute("productCount", products.count());
        model.addAttribute("stockValue", quants.totalValue());
        model.addAttribute("toReorder", toReorder);
        model.addAttribute("draftPurchases", purchases.countByState(PurchaseState.DRAFT));
        model.addAttribute("openProductions", productions.countByState(ProductionState.CONFIRMED));
        model.addAttribute("recentMoves", moves.findTop10ByStateOrderByDateDesc(MoveState.DONE));
        return "overview";
    }

    public static class OperationCard {
        private final PickingType type;
        private final long ready;
        private final long late;
        private final long draft;

        OperationCard(PickingType type, long ready, long late, long draft) {
            this.type = type;
            this.ready = ready;
            this.late = late;
            this.draft = draft;
        }

        public PickingType getType() { return type; }
        public long getReady() { return ready; }
        public long getLate() { return late; }
        public long getDraft() { return draft; }
    }
}
