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

import com.estoque.domain.PurchaseState;
import com.estoque.repository.PartnerRepository;
import com.estoque.repository.PickingRepository;
import com.estoque.repository.ProductRepository;
import com.estoque.repository.PurchaseOrderRepository;
import com.estoque.service.BusinessException;
import com.estoque.service.LocationService;
import com.estoque.service.PurchaseService;
import com.estoque.service.form.LineForm;
import com.estoque.service.form.PurchaseForm;

@Controller
@RequestMapping("/purchases")
public class PurchaseController {

    private static final int PAGE_SIZE = 80;

    private final PurchaseService purchaseService;
    private final PurchaseOrderRepository orders;
    private final PickingRepository pickings;
    private final ProductRepository products;
    private final PartnerRepository partners;
    private final LocationService locations;

    public PurchaseController(PurchaseService purchaseService, PurchaseOrderRepository orders, PickingRepository pickings,
                              ProductRepository products, PartnerRepository partners, LocationService locations) {
        this.purchaseService = purchaseService;
        this.orders = orders;
        this.pickings = pickings;
        this.products = products;
        this.partners = partners;
        this.locations = locations;
    }

    @GetMapping
    public String list(@RequestParam(required = false) PurchaseState state, @RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        List<PurchaseState> states = state == null ? Arrays.asList(PurchaseState.values()) : List.of(state);
        model.addAttribute("page", orders.search(states, q.trim(),
                PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id"))));
        model.addAttribute("state", state);
        model.addAttribute("q", q);
        model.addAttribute("states", PurchaseState.values());
        return "purchases/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        return form(purchaseService.newForm(), model);
    }

    @GetMapping("/{id}")
    public String edit(@PathVariable Long id, Model model) {
        var order = purchaseService.get(id);
        model.addAttribute("order", order);
        model.addAttribute("receipts", pickings.findByPurchaseOrderId(id));
        return form(PurchaseForm.from(order), model);
    }

    @PostMapping("/save")
    public String save(@Validated @ModelAttribute("form") PurchaseForm form, BindingResult result, Model model,
                       RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                var order = purchaseService.save(form);
                redirect.addFlashAttribute("success", "Pedido " + order.getReference() + " salvo.");
                return "redirect:/purchases/" + order.getId();
            } catch (BusinessException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        if (form.getId() != null) {
            model.addAttribute("order", purchaseService.get(form.getId()));
            model.addAttribute("receipts", pickings.findByPurchaseOrderId(form.getId()));
        }
        return form(form, model);
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable Long id, RedirectAttributes redirect) {
        var receipt = purchaseService.confirm(id);
        redirect.addFlashAttribute("success", "Pedido confirmado. Recebimento " + receipt.getReference() + " criado.");
        return "redirect:/purchases/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirect) {
        purchaseService.cancel(id);
        redirect.addFlashAttribute("success", "Pedido cancelado.");
        return "redirect:/purchases/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        purchaseService.delete(id);
        redirect.addFlashAttribute("success", "Pedido excluído.");
        return "redirect:/purchases";
    }

    private String form(PurchaseForm form, Model model) {
        if (form.getLines().isEmpty()) {
            form.getLines().add(new LineForm());
        }
        model.addAttribute("form", form);
        model.addAttribute("products", products.findByActiveTrueOrderByName());
        model.addAttribute("suppliers", partners.findBySupplierTrueAndActiveTrueOrderByName());
        model.addAttribute("locations", locations.internal());
        return "purchases/form";
    }
}
