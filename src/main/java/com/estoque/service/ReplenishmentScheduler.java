package com.estoque.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Executa periodicamente as regras de reposição com gatilho automático. */
@Component
@ConditionalOnProperty(name = "app.replenishment.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class ReplenishmentScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReplenishmentScheduler.class);

    private final ReplenishmentService replenishment;

    public ReplenishmentScheduler(ReplenishmentService replenishment) {
        this.replenishment = replenishment;
    }

    @Scheduled(cron = "${app.replenishment.cron:0 */15 * * * *}")
    public void run() {
        var references = replenishment.runAutomatic();
        if (!references.isEmpty()) {
            log.info("Reposição automática gerou/atualizou: {}", String.join(", ", references));
        }
    }
}
