package com.estoque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.estoque.domain.LocationUsage;
import com.estoque.domain.PickingState;
import com.estoque.domain.PickingType;
import com.estoque.domain.Product;
import com.estoque.domain.ProductRoute;
import com.estoque.domain.PurchaseState;
import com.estoque.domain.ReorderTrigger;
import com.estoque.repository.PartnerRepository;
import com.estoque.service.BusinessException;
import com.estoque.service.LocationService;
import com.estoque.service.PickingService;
import com.estoque.service.ProductService;
import com.estoque.service.PurchaseService;
import com.estoque.service.ReplenishmentService;
import com.estoque.service.StockService;
import com.estoque.service.form.LineForm;
import com.estoque.service.form.ProductForm;
import com.estoque.service.form.RuleForm;

/**
 * Fluxo completo contra um PostgreSQL real (Testcontainers): ajuste → entrega → reposição →
 * cotação → pedido → recebimento. Ignorado automaticamente se o Docker não estiver disponível.
 */
@SpringBootTest(properties = {"app.demo-data=false", "app.replenishment.scheduler-enabled=false"})
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class StockFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired ProductService productService;
    @Autowired PartnerRepository partners;
    @Autowired LocationService locationService;
    @Autowired StockService stockService;
    @Autowired PickingService pickingService;
    @Autowired PurchaseService purchaseService;
    @Autowired ReplenishmentService replenishmentService;

    @Test
    void fullReplenishmentCycle() {
        var stock = locationService.defaultStock();
        var supplier = partners.save(new com.estoque.domain.Partner("Fornecedor Teste", true, false));
        Product product = newProduct(supplier.getId());

        stockService.adjust(product, stock, new BigDecimal("10"));
        assertThat(stockService.onHand(product, stock)).isEqualByComparingTo("10");

        // Entrega pronta de 14 → previsão -4
        var delivery = pickingService.newForm(PickingType.DELIVERY);
        delivery.setLines(List.of(new LineForm(product.getId(), new BigDecimal("14"), null)));
        var picking = pickingService.save(delivery, "teste");
        pickingService.confirm(picking.getId());
        assertThat(stockService.forecast(product, stock)).isEqualByComparingTo("-4");

        // Regra mín. 5 / máx. 20 → pedir 24
        var ruleForm = new RuleForm();
        ruleForm.setProductId(product.getId());
        ruleForm.setLocationId(stock.getId());
        ruleForm.setMinQty(new BigDecimal("5"));
        ruleForm.setMaxQty(new BigDecimal("20"));
        ruleForm.setTrigger(ReorderTrigger.MANUAL);
        var rule = replenishmentService.save(ruleForm);
        assertThat(replenishmentService.line(rule).getToOrder()).isEqualByComparingTo("24");

        // "Pedir uma vez" cria a cotação; a quantidade em cotação zera a sugestão
        var refs = replenishmentService.order(List.of(rule.getId()), false);
        assertThat(refs).hasSize(1).first().asString().startsWith("P");
        assertThat(replenishmentService.line(rule).getToOrder()).isZero();

        // Confirmar o pedido gera o recebimento; validar atualiza o estoque e conclui o pedido
        var order = purchaseService.addToDraft(product, BigDecimal.ZERO, stock, "teste");
        var receipt = purchaseService.confirm(order.getId());
        assertThat(receipt.getState()).isEqualTo(PickingState.READY);
        pickingService.validate(receipt.getId());
        assertThat(stockService.onHand(product, stock)).isEqualByComparingTo("34");
        assertThat(purchaseService.get(order.getId()).getState()).isEqualTo(PurchaseState.DONE);

        // Entrega validada baixa o estoque
        pickingService.validate(picking.getId());
        assertThat(stockService.onHand(product, stock)).isEqualByComparingTo("20");
        assertThat(stockService.forecast(product, stock)).isEqualByComparingTo("20");
    }

    @Test
    void deliveryWithoutStockIsRejected() {
        var stock = locationService.defaultStock();
        Product product = newProduct(null);
        var form = pickingService.newForm(PickingType.DELIVERY);
        form.setScheduledDate(LocalDateTime.now());
        form.setLines(List.of(new LineForm(product.getId(), BigDecimal.ONE, null)));
        var picking = pickingService.save(form, "teste");

        assertThatThrownBy(() -> pickingService.validate(picking.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Estoque insuficiente");
        assertThat(stockService.onHand(product, stock)).isZero();
    }

    @Test
    void deliveryConsumesSublocations() {
        var stock = locationService.defaultStock();
        var shelf = locationService.create("Prateleira X", stock.getId(), LocationUsage.INTERNAL);
        Product product = newProduct(null);
        stockService.adjust(product, stock, new BigDecimal("2"));
        stockService.adjust(product, shelf, new BigDecimal("3"));

        var form = pickingService.newForm(PickingType.DELIVERY);
        form.setLines(List.of(new LineForm(product.getId(), new BigDecimal("4"), null)));
        pickingService.validate(pickingService.save(form, "teste").getId());

        assertThat(stockService.onHand(product, stock)).isEqualByComparingTo("1");
        assertThat(stockService.onHand(product, shelf)).isEqualByComparingTo("1");
    }

    private Product newProduct(Long supplierId) {
        var form = new ProductForm();
        form.setSku("T-" + UUID.randomUUID().toString().substring(0, 8));
        form.setName("Produto de teste");
        form.setCost(new BigDecimal("10.00"));
        form.setRoute(ProductRoute.BUY);
        form.setSupplierId(supplierId);
        return productService.save(form);
    }
}
