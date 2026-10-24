package com.estoque.web;

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

import com.estoque.domain.Partner;
import com.estoque.repository.PartnerRepository;
import com.estoque.service.BusinessException;

/** Fornecedores e clientes. */
@Controller
@RequestMapping("/partners")
public class PartnerController {

    private final PartnerRepository partners;

    public PartnerController(PartnerRepository partners) {
        this.partners = partners;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("page", partners.search(q.trim(), PageRequest.of(Math.max(page, 0), 80, Sort.by("name"))));
        model.addAttribute("q", q);
        return "partners/list";
    }

    @GetMapping("/new")
    public String create(Model model) {
        var partner = new Partner();
        partner.setSupplier(true);
        model.addAttribute("partner", partner);
        return "partners/form";
    }

    @GetMapping("/{id}")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("partner", partners.findById(id).orElseThrow(() -> BusinessException.notFound("Parceiro")));
        return "partners/form";
    }

    @PostMapping("/save")
    public String save(@Validated @ModelAttribute("partner") Partner partner, BindingResult result,
                       RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return "partners/form";
        }
        partners.save(partner);
        redirect.addFlashAttribute("success", "Parceiro salvo.");
        return "redirect:/partners";
    }
}
