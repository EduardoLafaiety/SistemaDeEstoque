package com.estoque.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Casos da tela de reposição do Odoo usada como referência. */
class ReplenishmentCalculationTest {

    @ParameterizedTest(name = "previsão {0}, mín {1}, máx {2}, múltiplo {3} → pedir {4}")
    @CsvSource({
            // forecast, min, max, multiple, expected
            "-1,  0,  0,  1, 1",   // regra manual 0/0 com previsão negativa
            "-6,  0,  0,  1, 6",
            "4,   5, 10,  1, 6",   // abaixo do mínimo: completa até o máximo
            "8,  10, 10,  1, 2",
            "12,  0,  0,  1, 0",   // acima do mínimo: nada a pedir
            "5,   5, 10,  1, 0",   // igual ao mínimo não dispara
            "1,   2,  2,  1, 1",
            "3,   5, 10,  4, 8",   // 7 arredondado para múltiplo de 4
            "0,   5,  3,  1, 5",   // máximo menor que o mínimo: usa o mínimo
    })
    void quantityToOrder(String forecast, String min, String max, String multiple, String expected) {
        BigDecimal result = ReplenishmentService.quantityToOrder(new BigDecimal(forecast), new BigDecimal(min),
                new BigDecimal(max), new BigDecimal(multiple), false);
        assertThat(result).isEqualByComparingTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "7, 5, 10, 3",   // "Pedir até o máximo" ignora o mínimo
            "10, 5, 10, 0",
            "-2, 0, 4, 6",
    })
    void orderToMax(String forecast, String min, String max, String expected) {
        BigDecimal result = ReplenishmentService.quantityToOrder(new BigDecimal(forecast), new BigDecimal(min),
                new BigDecimal(max), BigDecimal.ONE, true);
        assertThat(result).isEqualByComparingTo(expected);
    }
}
