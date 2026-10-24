package com.estoque.web;

import java.security.Principal;
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

import com.estoque.domain.PickingState;
import com.estoque.domain.PickingType;
import com.estoque.repository.PartnerRepository;
import com.estoque.repository.PickingRepository;
import com.estoque.repository.ProductRepository;
import com.estoque.service.BusinessException;
import com.estoque.service.LocationService;
import com.estoque.service.PickingService;
import com.estoque.service.form.PickingForm;

@Controller
@RequestMapping("/pickings")
public class PickingController {

    private static final int PAGE_SIZE = 80;

    private final PickingService pickingService;
    private final PickingRepository pickings;
    private final ProductRepository products;
    private final PartnerRepository partners;
    private final LocationService locations;

    public PickingController(PickingService pickingService, PickingRepository pickings, ProductRepository products,
                             PartnerRepository partners, LocationService locations) {
        this.pickingService = pickingService;
        this.pickings = pickings;
        this.products = products;
        this.partners = partners;
        this.locations = locations;
    }

    /** {@code state} vazio mostra as operações em aberto (rascunho e prontas). */
    @GetMapping
    public String list(@RequestParam PickingType type, @RequestParam(required = false) PickingState state,
                       @RequestParam(defaultValue = "false") boolean all, @RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        List<PickingState> states = state != null ? List.of(state)
                : all ? Arrays.asList(PickingState.values())
                : List.of(PickingState.DRAFT, PickingState.READY);
        var result = pickings.search(type, states, q.trim(),
                PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "scheduledDate")));
        model.addAttribute("type", type);
        model.addAttribute("page", result);
        model.addAttribute("state", state);
        model.addAttribute("all", all);
        model.addAttribute("q", q);
        model.addAttribute("states", PickingState.values());
        return "pickings/list";
    }

    @GetMapping("/new")
    public String create(@RequestParam PickingType type, Model model) {
        return form(pickingService.newForm(type), model);
    }

    @GetMapping("/{id}")
    public String edit(@PathVariable Long id, Model model) {
        var picking = pickingService.get(id);
        model.addAttribute("picking", picking);
        return form(PickingForm.from(picking), model);
    }

    @PostMapping("/save")
    public String save(@Validated @ModelAttribute("form") PickingForm form, BindingResult result, Model model,
                       Principal principal, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return formWithPicking(form, model);
        }
        try {
            var picking = pickingService.save(form, principal.getName());
            redirect.addFlashAttribute("success", "Operação " + picking.getReference() + " salva.");
            return "redirect:/pickings/" + picking.getId();
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return formWithPicking(form, model);
        }
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable Long id, RedirectAttributes redirect) {
        pickingService.confirm(id);
        redirect.addFlashAttribute("success", "Operação marcada como pronta.");
        return "redirect:/pickings/" + id;
    }

    @PostMapping("/{id}/validate")
    public String validate(@PathVariable Long id, RedirectAttributes redirect) {
        pickingService.validate(id);
        redirect.addFlashAttribute("success", "Operação validada: estoque atualizado.");
        return "redirect:/pickings/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirect) {
        pickingService.cancel(id);
        redirect.addFlashAttribute("success", "Operação cancelada.");
        return "redirect:/pickings/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        var type = pickingService.get(id).getType();
        pickingService.delete(id);
        redirect.addFlashAttribute("success", "Operação excluída.");
        return "redirect:/pickings?type=" + type;
    }

    private String formWithPicking(PickingForm form, Model model) {
        if (form.getId() != null) {
            model.addAttribute("picking", pickingService.get(form.getId()));
        }
        return form(form, model);
    }

    private String form(PickingForm form, Model model) {
        if (form.getLines().isEmpty()) {
            form.getLines().add(new com.estoque.service.form.LineForm());
        }
        model.addAttribute("form", form);
        model.addAttribute("products", products.findByActiveTrueOrderByName());
        model.addAttribute("partners", partners.findByActiveTrueOrderByName());
        model.addAttribute("locations", locations.active());
        return "pickings/form";
    }
}
