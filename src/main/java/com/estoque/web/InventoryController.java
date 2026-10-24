package com.estoque.web;

import java.math.BigDecimal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.estoque.repository.ProductRepository;
import com.estoque.repository.StockQuantRepository;
import com.estoque.service.LocationService;
import com.estoque.service.ProductService;
import com.estoque.service.StockService;

/** Inventário físico: informar a quantidade contada de cada produto em um local. */
@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private final StockQuantRepository quants;
    private final ProductRepository products;
    private final ProductService productService;
    private final LocationService locations;
    private final StockService stockService;

    public InventoryController(StockQuantRepository quants, ProductRepository products, ProductService productService,
                               LocationService locations, StockService stockService) {
        this.quants = quants;
        this.products = products;
        this.productService = productService;
        this.locations = locations;
        this.stockService = stockService;
    }

    @GetMapping
    public String view(@RequestParam(required = false) Long location, Model model) {
        var selected = location == null ? locations.defaultStock() : locations.get(location);
        model.addAttribute("location", selected);
        model.addAttribute("locations", locations.internal());
        model.addAttribute("quants", quants.findByLocation(selected.getId()));
        model.addAttribute("products", products.findByActiveTrueOrderByName());
        return "inventory/adjust";
    }

    @PostMapping("/apply")
    public String apply(@RequestParam Long productId, @RequestParam Long locationId, @RequestParam BigDecimal counted,
                        RedirectAttributes redirect) {
        var product = productService.get(productId);
        var move = stockService.adjust(product, locations.get(locationId), counted);
        redirect.addFlashAttribute("success", move == null
                ? "Sem diferença para " + product.getDisplayName() + "."
                : "Inventário de " + product.getDisplayName() + " ajustado.");
        return "redirect:/inventory?location=" + locationId;
    }
}
