package com.estoque.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class ViewHelperTest {

    private final ViewHelper fmt = new ViewHelper();

    @Test
    void formatsQuantitiesAndMoneyInBrazilianPortuguese() {
        assertThat(fmt.qty(new BigDecimal("1234.5"))).isEqualTo("1.234,50");
        assertThat(fmt.qty(new BigDecimal("-1"))).isEqualTo("-1,00");
        assertThat(fmt.qty(new BigDecimal("0.125"))).isEqualTo("0,125");
        assertThat(fmt.money(new BigDecimal("1490"))).isEqualTo("R$ 1.490,00");
        assertThat(fmt.input(new BigDecimal("10.000"))).isEqualTo("10");
    }

    @Test
    void formatsDates() {
        assertThat(fmt.date(LocalDate.of(2026, 10, 24))).isEqualTo("24/10/2026");
        assertThat(fmt.dateTime(LocalDateTime.of(2026, 10, 24, 9, 5))).isEqualTo("24/10/2026 09:05");
        assertThat(fmt.date(null)).isEmpty();
    }
}
