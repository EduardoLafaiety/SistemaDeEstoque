package com.estoque.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

import com.estoque.domain.ReorderTrigger;
import com.estoque.repository.ProductRepository;
import com.estoque.service.BusinessException;
import com.estoque.service.LocationService;
import com.estoque.service.ReplenishmentFilter;
import com.estoque.service.ReplenishmentService;
import com.estoque.service.form.RuleForm;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/replenishment")
public class ReplenishmentController {

    private final ReplenishmentService replenishment;
    private final ProductRepository products;
    private final LocationService locations;

    public ReplenishmentController(ReplenishmentService replenishment, ProductRepository products, LocationService locations) {
        this.replenishment = replenishment;
        this.products = products;
        this.locations = locations;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Long location,
                       @RequestParam(required = false) ReorderTrigger trigger,
                       @RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "false") boolean toOrder,
                       @RequestParam(defaultValue = "false") boolean snoozed,
                       Model model) {
        replenishment.syncManualRules();
        var view = replenishment.view(new ReplenishmentFilter(location, trigger, q, toOrder, snoozed));
        model.addAttribute("view", view);
        model.addAttribute("location", location);
        model.addAttribute("trigger", trigger);
        model.addAttribute("q", q);
        model.addAttribute("toOrder", toOrder);
        model.addAttribute("snoozed", snoozed);
        return "replenishment/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        var form = new RuleForm();
        form.setLocationId(locations.defaultStock().getId());
        return form(form, model);
    }

    @GetMapping("/{id}")
    public String edit(@PathVariable Long id, Model model) {
        return form(RuleForm.from(replenishment.get(id)), model);
    }

    @PostMapping("/save")
    public String save(@Validated @ModelAttribute("form") RuleForm form, BindingResult result, Model model,
                       RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return form(form, model);
        }
        try {
            replenishment.save(form);
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return form(form, model);
        }
        redirect.addFlashAttribute("success", "Regra de reposição salva.");
        return "redirect:/replenishment";
    }

    @PostMapping("/{id}/quantities")
    public String updateQuantities(@PathVariable Long id, @RequestParam BigDecimal minQty, @RequestParam BigDecimal maxQty,
                                   HttpServletRequest request) {
        replenishment.updateQuantities(id, minQty, maxQty);
        return "redirect:" + GlobalControllerAdvice.back(request);
    }

    @PostMapping("/order")
    public String order(@RequestParam("ids") List<Long> ids, @RequestParam(defaultValue = "false") boolean toMax,
                        HttpServletRequest request, RedirectAttributes redirect) {
        var references = replenishment.order(ids, toMax);
        redirect.addFlashAttribute("success", "Pedidos gerados/atualizados: " + String.join(", ", references));
        return "redirect:" + GlobalControllerAdvice.back(request);
    }

    @PostMapping("/snooze")
    public String snooze(@RequestParam("ids") List<Long> ids, @RequestParam String period,
                         @RequestParam(required = false) LocalDate until,
                         HttpServletRequest request, RedirectAttributes redirect) {
        LocalDate date = switch (period) {
            case "day" -> LocalDate.now().plusDays(1);
            case "week" -> LocalDate.now().plusWeeks(1);
            case "month" -> LocalDate.now().plusMonths(1);
            default -> until;
        };
        replenishment.snooze(ids, date);
        redirect.addFlashAttribute("success", ids.size() + " linha(s) adiada(s).");
        return "redirect:" + GlobalControllerAdvice.back(request);
    }

    @PostMapping("/unsnooze")
    public String unsnooze(@RequestParam("ids") List<Long> ids, HttpServletRequest request) {
        replenishment.unsnooze(ids);
        return "redirect:" + GlobalControllerAdvice.back(request);
    }

    @PostMapping("/trigger")
    public String trigger(@RequestParam("ids") List<Long> ids, @RequestParam ReorderTrigger trigger,
                          HttpServletRequest request, RedirectAttributes redirect) {
        replenishment.setTrigger(ids, trigger);
        redirect.addFlashAttribute("success", "Gatilho alterado para " + trigger.getLabel().toLowerCase() + ".");
        return "redirect:" + GlobalControllerAdvice.back(request);
    }

    @PostMapping("/delete")
    public String delete(@RequestParam("ids") List<Long> ids, HttpServletRequest request, RedirectAttributes redirect) {
        replenishment.delete(ids);
        redirect.addFlashAttribute("success", ids.size() + " regra(s) excluída(s).");
        return "redirect:" + GlobalControllerAdvice.back(request);
    }

    @PostMapping("/run")
    public String runAutomatic(RedirectAttributes redirect) {
        var references = replenishment.runAutomatic();
        redirect.addFlashAttribute("success", references.isEmpty()
                ? "Nenhuma regra automática precisava de reposição."
                : "Reposição automática: " + String.join(", ", references));
        return "redirect:/replenishment";
    }

    private String form(RuleForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("products", products.findByActiveTrueOrderByName());
        model.addAttribute("locations", locations.internal());
        model.addAttribute("triggers", ReorderTrigger.values());
        return "replenishment/form";
    }
}
