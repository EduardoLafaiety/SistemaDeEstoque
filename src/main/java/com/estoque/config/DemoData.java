package com.estoque.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.estoque.domain.Category;
import com.estoque.domain.Location;
import com.estoque.domain.LocationUsage;
import com.estoque.domain.Partner;
import com.estoque.domain.Product;
import com.estoque.domain.PickingType;
import com.estoque.domain.ProductRoute;
import com.estoque.domain.ReorderTrigger;
import com.estoque.repository.CategoryRepository;
import com.estoque.repository.PartnerRepository;
import com.estoque.repository.ProductRepository;
import com.estoque.service.LocationService;
import com.estoque.service.PickingService;
import com.estoque.service.ProductService;
import com.estoque.service.PurchaseService;
import com.estoque.service.ReplenishmentService;
import com.estoque.service.StockService;
import com.estoque.service.form.LineForm;
import com.estoque.service.form.ProductForm;
import com.estoque.service.form.PurchaseForm;
import com.estoque.service.form.RuleForm;

/**
 * Dados de exemplo inspirados na tela de reposição do Odoo. Tudo é criado pelos serviços,
 * então saldos e movimentos ficam consistentes.
 */
@Component
public class DemoData {

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final PartnerRepository partners;
    private final ProductService productService;
    private final LocationService locationService;
    private final StockService stockService;
    private final PickingService pickingService;
    private final PurchaseService purchaseService;
    private final ReplenishmentService replenishmentService;

    public DemoData(ProductRepository products, CategoryRepository categories, PartnerRepository partners,
                    ProductService productService, LocationService locationService, StockService stockService,
                    PickingService pickingService, PurchaseService purchaseService,
                    ReplenishmentService replenishmentService) {
        this.products = products;
        this.categories = categories;
        this.partners = partners;
        this.productService = productService;
        this.locationService = locationService;
        this.stockService = stockService;
        this.pickingService = pickingService;
        this.purchaseService = purchaseService;
        this.replenishmentService = replenishmentService;
    }

    @Transactional
    public boolean loadIfEmpty() {
        if (products.count() > 0) {
            return false;
        }
        var furniture = categories.save(new Category("Móveis"));
        var office = categories.save(new Category("Escritório"));

        var woodCo = save(new Partner("Madeiras Brasil Ltda", true, false), "compras@madeirasbrasil.com.br", "12.345.678/0001-90");
        var officeCo = save(new Partner("Office Supply S.A.", true, false), "vendas@officesupply.com.br", "98.765.432/0001-10");
        var customerA = save(new Partner("Loja Centro", false, true), "contato@lojacentro.com.br", null);
        var customerB = save(new Partner("Escritórios Alfa", false, true), "compras@alfa.com.br", null);

        Location stock = locationService.defaultStock();
        Location shelfA = locationService.create("Prateleira A", stock.getId(), LocationUsage.INTERNAL);
        Location shelfB = locationService.create("Prateleira B", stock.getId(), LocationUsage.INTERNAL);

        var cornerDesk = product("E-COM06", "Mesa de Canto", furniture, "320.00", "590.00", ProductRoute.BUY, woodCo, 7);
        var largeDesk = product("E-COM09", "Mesa Grande", furniture, "780.00", "1490.00", ProductRoute.BUY, woodCo, 10);
        var flipover = product("FURN_9001", "Flipchart", office, "150.00", "289.00", ProductRoute.BUY, officeCo, 5);
        var table = product("FURN_9666", "Mesa de Reunião", furniture, "950.00", "1890.00", ProductRoute.MANUFACTURE, null, 4);
        var chair = product("FURN_7777", "Cadeira de Escritório", office, "210.00", "449.00", ProductRoute.BUY, officeCo, 5);
        var lamp = product("FURN_8888", "Luminária de Mesa", office, "45.00", "99.90", ProductRoute.BUY, officeCo, 3);
        var drawer = product("FURN_8900", "Gaveteiro Preto", furniture, "260.00", "520.00", ProductRoute.MANUFACTURE, null, 6);

        stockService.adjust(cornerDesk, stock, qty("4"));
        stockService.adjust(largeDesk, stock, qty("1"));
        stockService.adjust(flipover, stock, qty("5"));
        stockService.adjust(table, stock, qty("2"));
        stockService.adjust(chair, shelfA, qty("4"));
        stockService.adjust(lamp, shelfA, qty("8"));
        stockService.adjust(drawer, shelfA, qty("12"));
        stockService.adjust(flipover, shelfA, qty("5"));
        stockService.adjust(cornerDesk, shelfB, qty("4"));
        stockService.adjust(largeDesk, shelfB, qty("1"));
        stockService.adjust(table, shelfB, qty("2"));

        rule(chair, shelfA, "5", "10", ReorderTrigger.AUTO);
        rule(lamp, shelfA, "10", "10", ReorderTrigger.MANUAL);
        rule(drawer, shelfA, "0", "0", ReorderTrigger.MANUAL);
        rule(largeDesk, shelfB, "2", "2", ReorderTrigger.MANUAL);
        rule(table, shelfB, "5", "10", ReorderTrigger.AUTO);

        // Entregas prontas que deixam a previsão negativa no estoque principal.
        delivery(customerA, stock, List.of(line(cornerDesk, "5"), line(largeDesk, "5"), line(flipover, "11")));
        delivery(customerB, stock, List.of(line(table, "3")));

        // Uma cotação em aberto para mostrar o fluxo de compras.
        var po = new PurchaseForm();
        po.setPartnerId(officeCo.getId());
        po.setDestLocationId(stock.getId());
        po.setExpectedDate(LocalDate.now().plusDays(5));
        po.getLines().add(new LineForm(lamp.getId(), qty("20"), lamp.getCost()));
        purchaseService.save(po);

        replenishmentService.syncManualRules();
        return true;
    }

    private Partner save(Partner partner, String email, String document) {
        partner.setEmail(email);
        partner.setDocument(document);
        return partners.save(partner);
    }

    private Product product(String sku, String name, Category category, String cost, String price, ProductRoute route,
                            Partner supplier, int leadTime) {
        var form = new ProductForm();
        form.setSku(sku);
        form.setName(name);
        form.setCategoryId(category.getId());
        form.setCost(new BigDecimal(cost));
        form.setSalePrice(new BigDecimal(price));
        form.setRoute(route);
        form.setSupplierId(supplier == null ? null : supplier.getId());
        form.setLeadTimeDays(leadTime);
        return productService.save(form);
    }

    private void rule(Product product, Location location, String min, String max, ReorderTrigger trigger) {
        var form = new RuleForm();
        form.setProductId(product.getId());
        form.setLocationId(location.getId());
        form.setMinQty(qty(min));
        form.setMaxQty(qty(max));
        form.setTrigger(trigger);
        replenishmentService.save(form);
    }

    private void delivery(Partner customer, Location stock, List<LineForm> lines) {
        var form = pickingService.newForm(PickingType.DELIVERY);
        form.setPartnerId(customer.getId());
        form.setSourceLocationId(stock.getId());
        form.setScheduledDate(LocalDateTime.now().plusDays(2).withSecond(0).withNano(0));
        form.setLines(lines);
        var picking = pickingService.save(form, "admin");
        pickingService.confirm(picking.getId());
    }

    private static LineForm line(Product product, String quantity) {
        return new LineForm(product.getId(), qty(quantity), null);
    }

    private static BigDecimal qty(String value) {
        return new BigDecimal(value);
    }
}
