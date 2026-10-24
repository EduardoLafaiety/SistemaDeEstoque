package com.estoque.web;

import java.util.Arrays;
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

import com.estoque.domain.ProductionState;
import com.estoque.repository.ManufacturingOrderRepository;
import com.estoque.repository.ProductRepository;
import com.estoque.service.BusinessException;
import com.estoque.service.LocationService;
import com.estoque.service.ManufacturingService;
import com.estoque.service.form.ProductionForm;

@Controller
@RequestMapping("/production")
public class ProductionController {

    private static final int PAGE_SIZE = 80;

    private final ManufacturingService manufacturingService;
    private final ManufacturingOrderRepository orders;
    private final ProductRepository products;
    private final LocationService locations;

    public ProductionController(ManufacturingService manufacturingService, ManufacturingOrderRepository orders,
                                ProductRepository products, LocationService locations) {
        this.manufacturingService = manufacturingService;
        this.orders = orders;
        this.products = products;
        this.locations = locations;
    }

    @GetMapping
    public String list(@RequestParam(required = false) ProductionState state, @RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        List<ProductionState> states = state == null ? Arrays.asList(ProductionState.values()) : List.of(state);
        model.addAttribute("page", orders.search(states, q.trim(),
                PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id"))));
        model.addAttribute("state", state);
        model.addAttribute("q", q);
        model.addAttribute("states", ProductionState.values());
        return "production/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        return form(manufacturingService.newForm(), model);
    }

    @GetMapping("/{id}")
    public String edit(@PathVariable Long id, Model model) {
        var order = manufacturingService.get(id);
        model.addAttribute("order", order);
        return form(ProductionForm.from(order), model);
    }

    @PostMapping("/save")
    public String save(@Validated @ModelAttribute("form") ProductionForm form, BindingResult result, Model model,
                       RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                var order = manufacturingService.save(form);
                redirect.addFlashAttribute("success", "Ordem " + order.getReference() + " salva.");
                return "redirect:/production/" + order.getId();
            } catch (BusinessException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        if (form.getId() != null) {
            model.addAttribute("order", manufacturingService.get(form.getId()));
        }
        return form(form, model);
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable Long id, RedirectAttributes redirect) {
        manufacturingService.confirm(id);
        redirect.addFlashAttribute("success", "Ordem confirmada.");
        return "redirect:/production/" + id;
    }

    @PostMapping("/{id}/produce")
    public String produce(@PathVariable Long id, RedirectAttributes redirect) {
        manufacturingService.produce(id);
        redirect.addFlashAttribute("success", "Produção concluída: estoque atualizado.");
        return "redirect:/production/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirect) {
        manufacturingService.cancel(id);
        redirect.addFlashAttribute("success", "Ordem cancelada.");
        return "redirect:/production/" + id;
    }

    private String form(ProductionForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("products", products.findByActiveTrueOrderByName());
        model.addAttribute("locations", locations.internal());
        return "production/form";
    }
}
