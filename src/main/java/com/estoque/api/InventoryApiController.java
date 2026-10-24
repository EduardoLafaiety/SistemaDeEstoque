package com.estoque.api;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.estoque.api.ApiDtos.ErrorDto;
import com.estoque.api.ApiDtos.OrderResult;
import com.estoque.api.ApiDtos.ProductDto;
import com.estoque.api.ApiDtos.ReplenishmentDto;
import com.estoque.api.ApiDtos.StockDto;
import com.estoque.repository.ProductRepository;
import com.estoque.repository.StockQuantRepository;
import com.estoque.service.BusinessException;
import com.estoque.service.LocationService;
import com.estoque.service.ProductService;
import com.estoque.service.ReplenishmentFilter;
import com.estoque.service.ReplenishmentService;
import com.estoque.service.StockService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Estoque", description = "Consulta de produtos, saldos e reposição")
public class InventoryApiController {

    private final ProductRepository products;
    private final ProductService productService;
    private final StockQuantRepository quants;
    private final StockService stockService;
    private final LocationService locations;
    private final ReplenishmentService replenishment;

    public InventoryApiController(ProductRepository products, ProductService productService, StockQuantRepository quants,
                                  StockService stockService, LocationService locations,
                                  ReplenishmentService replenishment) {
        this.products = products;
        this.productService = productService;
        this.quants = quants;
        this.stockService = stockService;
        this.locations = locations;
        this.replenishment = replenishment;
    }

    @Operation(summary = "Lista produtos ativos com saldo total")
    @GetMapping("/products")
    public Page<ProductDto> products(@RequestParam(defaultValue = "") String q,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "50") int size) {
        return products.search(q.trim(), 0, true, PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 200), Sort.by("name")))
                .map(p -> ProductDto.of(p, stockService.onHandTotal(p)));
    }

    @Operation(summary = "Detalha um produto")
    @GetMapping("/products/{id}")
    public ProductDto product(@PathVariable Long id) {
        var product = productService.get(id);
        return ProductDto.of(product, stockService.onHandTotal(product));
    }

    @Operation(summary = "Saldos por local interno (opcionalmente filtrado por local e busca)")
    @GetMapping("/stock")
    public Page<StockDto> stock(@RequestParam(required = false) Long location, @RequestParam(defaultValue = "") String q,
                                @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        String path = location == null ? "" : locations.get(location).getFullName();
        return quants.searchInternal(path, q.trim(),
                        PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 200), Sort.by("product.name")))
                .map(StockDto::of);
    }

    @Operation(summary = "Linhas de reposição (regras mín./máx. com previsão e quantidade a pedir)")
    @GetMapping("/replenishment")
    public List<ReplenishmentDto> replenishment(@RequestParam(defaultValue = "false") boolean toOrder) {
        return replenishment.view(new ReplenishmentFilter(null, null, null, toOrder, false))
                .getLines().stream().map(ReplenishmentDto::of).toList();
    }

    @Operation(summary = "Gera cotações/ordens para as regras informadas (\"Pedir uma vez\")")
    @PostMapping("/replenishment/order")
    public OrderResult order(@RequestBody List<Long> ruleIds, @RequestParam(defaultValue = "false") boolean toMax) {
        return new OrderResult(replenishment.order(ruleIds, toMax));
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ErrorDto handle(BusinessException e) {
        return new ErrorDto(e.getMessage());
    }
}
