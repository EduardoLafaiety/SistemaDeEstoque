package com.estoque.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.estoque.repository.StockMoveRepository;
import com.estoque.repository.StockQuantRepository;
import com.estoque.service.LocationService;

import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private static final int PAGE_SIZE = 80;
    private static final Sort STOCK_SORT = Sort.by("product.name", "location.fullName");

    private final StockQuantRepository quants;
    private final StockMoveRepository moves;
    private final LocationService locations;
    private final ViewHelper fmt;

    public ReportController(StockQuantRepository quants, StockMoveRepository moves, LocationService locations, ViewHelper fmt) {
        this.quants = quants;
        this.moves = moves;
        this.locations = locations;
        this.fmt = fmt;
    }

    @GetMapping("/stock")
    public String stock(@RequestParam(required = false) Long location, @RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "0") int page, Model model) {
        String path = location == null ? "" : locations.get(location).getFullName();
        model.addAttribute("page", quants.searchInternal(path, q.trim(),
                PageRequest.of(Math.max(page, 0), PAGE_SIZE, STOCK_SORT)));
        model.addAttribute("totalValue", quants.totalValue());
        model.addAttribute("locations", locations.active());
        model.addAttribute("location", location);
        model.addAttribute("q", q);
        return "reports/stock";
    }

    /** Exporta o relatório de estoque em CSV (separador ";" para abrir direto no Excel em pt-BR). */
    @GetMapping("/stock.csv")
    public void stockCsv(@RequestParam(required = false) Long location, @RequestParam(defaultValue = "") String q,
                         HttpServletResponse response) throws IOException {
        String path = location == null ? "" : locations.get(location).getFullName();
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"estoque-" + LocalDate.now() + ".csv\"");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        PrintWriter out = response.getWriter();
        out.write('﻿');
        out.println("Código;Produto;Local;Quantidade;Unidade;Custo unitário;Valor");
        for (var quant : quants.searchInternal(path, q.trim(), Pageable.unpaged(STOCK_SORT))) {
            var p = quant.getProduct();
            out.println(String.join(";", csv(p.getSku()), csv(p.getName()), csv(quant.getLocation().getFullName()),
                    fmt.qty(quant.getQuantity()), csv(p.getUom()), fmt.qty(p.getCost()), fmt.qty(quant.getValue())));
        }
        out.flush();
    }

    @GetMapping("/moves")
    public String moves(@RequestParam(defaultValue = "") String q, @RequestParam(required = false) LocalDate from,
                        @RequestParam(required = false) LocalDate to, @RequestParam(defaultValue = "0") long product,
                        @RequestParam(defaultValue = "0") int page, Model model) {
        LocalDate start = from == null ? LocalDate.now().minusDays(30) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        model.addAttribute("page", moves.searchDone(q.trim(), product, start.atStartOfDay(), end.plusDays(1).atStartOfDay(),
                PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "date"))));
        model.addAttribute("q", q);
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        return "reports/moves";
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        // Evita que planilhas interpretem o conteúdo como fórmula.
        if (!escaped.isEmpty() && "=+-@".indexOf(escaped.charAt(0)) >= 0) {
            escaped = "'" + escaped;
        }
        return "\"" + escaped + "\"";
    }
}
