package com.estoque.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param admin    credenciais do administrador criado no primeiro start (quando não há usuários)
 * @param demoData carrega produtos, regras e operações de exemplo em um banco vazio
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Admin admin, boolean demoData) {

    public AppProperties {
        if (admin == null) {
            admin = new Admin(null, null);
        }
    }

    public record Admin(String username, String password) {
        public Admin {
            if (username == null || username.isBlank()) {
                username = "admin";
            }
            if (password == null || password.isBlank()) {
                password = "admin123";
            }
        }
    }
}
