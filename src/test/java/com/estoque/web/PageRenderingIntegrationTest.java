package com.estoque.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.estoque.repository.ManufacturingOrderRepository;
import com.estoque.repository.PickingRepository;
import com.estoque.repository.ProductRepository;
import com.estoque.repository.PurchaseOrderRepository;

/**
 * Renderiza as telas Thymeleaf com os dados de demonstração. Erros de template (ex.: expressões
 * proibidas pelo modo restrito do Thymeleaf em th:href, th:attr ou parâmetros de fragmento)
 * só aparecem em tempo de execução, então cada página é aberta de fato.
 */
@SpringBootTest(properties = {"app.demo-data=true", "app.replenishment.scheduler-enabled=false"})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@WithMockUser(username = "admin", roles = "ADMIN")
class PageRenderingIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired PickingRepository pickings;
    @Autowired PurchaseOrderRepository purchases;
    @Autowired ManufacturingOrderRepository production;

    @Test
    void allMainPagesRender() throws Exception {
        List<String> urls = new ArrayList<>(List.of(
                "/", "/replenishment", "/replenishment?toOrder=true", "/replenishment?snoozed=true",
                "/replenishment/new", "/products", "/products?archived=true", "/products/new",
                "/pickings?type=RECEIPT", "/pickings/new?type=RECEIPT", "/pickings/new?type=DELIVERY",
                "/purchases", "/purchases/new", "/production", "/production/new",
                "/partners", "/partners/new", "/inventory", "/reports/stock", "/reports/moves",
                "/config/warehouses", "/config/locations", "/config/categories", "/config/users",
                "/config/users/new"));
        products.findAll().stream().findFirst().ifPresent(p -> {
            urls.add("/products/" + p.getId());
            urls.add("/products/" + p.getId() + "/forecast");
        });
        pickings.findAll().forEach(p -> urls.add("/pickings/" + p.getId()));
        purchases.findAll().forEach(p -> urls.add("/purchases/" + p.getId()));
        production.findAll().forEach(m -> urls.add("/production/" + m.getId()));

        for (String url : urls) {
            mvc.perform(get(url)).andExpect(status().isOk());
        }
    }
}
