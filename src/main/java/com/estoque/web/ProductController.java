package com.estoque.web;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.estoque.domain.Location;
import com.estoque.domain.MoveState;
import com.estoque.domain.ProductRoute;
import com.estoque.domain.StockMove;
import com.estoque.repository.CategoryRepository;
import com.estoque.repository.PartnerRepository;
import com.estoque.repository.ProductRepository;
import com.estoque.repository.ReorderRuleRepository;
import com.estoque.repository.StockMoveRepository;
import com.estoque.repository.StockQuantRepository;
import com.estoque.service.BusinessException;
import com.estoque.service.LocationService;
import com.estoque.service.ProductService;
import com.estoque.service.StockService;
import com.estoque.service.form.ProductForm;

@Controller
@RequestMapping("/products")
public class ProductController {

    private static final int PAGE_SIZE = 80;

    private final ProductService productService;
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final PartnerRepository partners;
    private final StockQuantRepository quants;
    private final StockMoveRepository moves;
    private final ReorderRuleRepository rules;
    private final StockService stockService;
    private final LocationService locationService;

    public ProductController(ProductService productService, ProductRepository products, CategoryRepository categories,
                             PartnerRepository partners, StockQuantRepository quants, StockMoveRepository moves,
                             ReorderRuleRepository rules, StockService stockService, LocationService locationService) {
        this.productService = productService;
        this.products = products;
        this.categories = categories;
        this.partners = partners;
        this.quants = quants;
        this.moves = moves;
        this.rules = rules;
        this.stockService = stockService;
        this.locationService = locationService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") long category,
                       @RequestParam(defaultValue = "false") boolean archived, @RequestParam(defaultValue = "0") int page,
                       Model model) {
        var result = products.search(q.trim(), category, !archived,
                PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("name")));
        var onHand = new java.util.HashMap<Long, BigDecimal>();
        result.forEach(p -> onHand.put(p.getId(), stockService.onHandTotal(p)));
        model.addAttribute("page", result);
        model.addAttribute("onHand", onHand);
        model.addAttribute("categories", categories.findAllByOrderByName());
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("archived", archived);
        return "products/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        return form(new ProductForm(), model);
    }

    @GetMapping("/{id}")
    public String edit(@PathVariable Long id, Model model) {
        var product = productService.get(id);
        model.addAttribute("product", product);
        model.addAttribute("quants", quants.findByProduct(id));
        model.addAttribute("onHand", stockService.onHandTotal(product));
        model.addAttribute("rules", rules.findByProductIdOrderById(id));
        model.addAttribute("moves", moves.findTop20ByProductIdAndStateOrderByDateDesc(id, MoveState.DONE));
        return form(ProductForm.from(product), model);
    }

    @PostMapping("/save")
    public String save(@Validated @ModelAttribute("form") ProductForm form, BindingResult result, Model model,
                       RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return form(form, model);
        }
        try {
            var product = productService.save(form);
            redirect.addFlashAttribute("success", "Produto salvo.");
            return "redirect:/products/" + product.getId();
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return form(form, model);
        }
    }

    @PostMapping("/{id}/archive")
    public String archive(@PathVariable Long id, @RequestParam boolean active, RedirectAttributes redirect) {
        productService.setActive(id, active);
        redirect.addFlashAttribute("success", active ? "Produto restaurado." : "Produto arquivado.");
        return "redirect:/products/" + id;
    }

    /** Relatório de previsão: saldo atual e projeção a cada movimento pendente. */
    @GetMapping("/{id}/forecast")
    public String forecast(@PathVariable Long id, @RequestParam(required = false) Long location, Model model) {
        var product = productService.get(id);
        Location selected = location == null ? locationService.defaultStock() : locationService.get(location);
        BigDecimal onHand = stockService.onHand(product, selected);

        var rows = new ArrayList<ForecastRow>();
        BigDecimal balance = onHand;
        for (StockMove move : moves.findPending(id, selected.getFullName())) {
            boolean incoming = move.getDestLocation().isChildOf(selected);
            boolean outgoing = move.getSourceLocation().isChildOf(selected);
            if (incoming == outgoing) {
                continue; // movimento interno à subárvore: não altera a previsão
            }
            BigDecimal delta = incoming ? move.getQuantity() : move.getQuantity().negate();
            balance = balance.add(delta);
            rows.add(new ForecastRow(move, delta, balance));
        }
        model.addAttribute("product", product);
        model.addAttribute("location", selected);
        model.addAttribute("locations", locationService.internal());
        model.addAttribute("onHand", onHand);
        model.addAttribute("forecast", balance);
        model.addAttribute("rows", rows);
        return "products/forecast";
    }

    private String form(ProductForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("categories", categories.findAllByOrderByName());
        model.addAttribute("suppliers", partners.findBySupplierTrueAndActiveTrueOrderByName());
        model.addAttribute("routes", ProductRoute.values());
        if (!model.containsAttribute("quants")) {
            model.addAttribute("quants", List.of());
            model.addAttribute("rules", List.of());
            model.addAttribute("moves", List.of());
        }
        return "products/form";
    }

    public static class ForecastRow {
        private final StockMove move;
        private final BigDecimal delta;
        private final BigDecimal balance;

        ForecastRow(StockMove move, BigDecimal delta, BigDecimal balance) {
            this.move = move;
            this.delta = delta;
            this.balance = balance;
        }

        public StockMove getMove() { return move; }
        public BigDecimal getDelta() { return delta; }
        public BigDecimal getBalance() { return balance; }
    }
}
