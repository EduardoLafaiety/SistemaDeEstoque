package com.estoque.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.estoque.service.UserService;

/** Cria o administrador inicial e, se habilitado, os dados de demonstração. */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AppProperties properties;
    private final UserService userService;
    private final DemoData demoData;

    public DataInitializer(AppProperties properties, UserService userService, DemoData demoData) {
        this.properties = properties;
        this.userService = userService;
        this.demoData = demoData;
    }

    @Override
    public void run(ApplicationArguments args) {
        var admin = properties.admin();
        if (userService.createInitialAdmin(admin.username(), admin.password())) {
            log.warn("Usuário administrador '{}' criado. Altere a senha após o primeiro acesso.", admin.username());
        }
        if (properties.demoData() && demoData.loadIfEmpty()) {
            log.info("Dados de demonstração carregados.");
        }
    }
}
