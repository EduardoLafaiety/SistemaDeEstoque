package com.estoque.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Formatação pt-BR e utilidades para os templates, acessível como {@code ${@fmt...}}. */
@Component("fmt")
public class ViewHelper {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public String qty(BigDecimal value) {
        if (value == null) {
            return "";
        }
        var format = new DecimalFormat("#,##0.00#", DecimalFormatSymbols.getInstance(PT_BR));
        return format.format(value.setScale(3, RoundingMode.HALF_UP));
    }

    public String money(BigDecimal value) {
        if (value == null) {
            return "";
        }
        var format = new DecimalFormat("'R$' #,##0.00", DecimalFormatSymbols.getInstance(PT_BR));
        return format.format(value);
    }

    /** Valor para campos {@code <input type="number">} (ponto decimal, sem zeros à direita). */
    public String input(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    public String date(LocalDate value) {
        return value == null ? "" : DATE.format(value);
    }

    public String dateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME.format(value);
    }

    /** Parâmetros atuais da requisição, exceto busca e página (para manter filtros ao pesquisar). */
    public Map<String, String> filterParams() {
        var result = new LinkedHashMap<String, String>();
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            attrs.getRequest().getParameterMap().forEach((name, values) -> {
                if (!name.equals("q") && !name.equals("page") && values.length > 0) {
                    result.put(name, values[0]);
                }
            });
        }
        return result;
    }

    /** URL da página atual trocando apenas o parâmetro {@code page}. */
    public String pageUrl(int page) {
        return ServletUriComponentsBuilder.fromCurrentRequest().replaceQueryParam("page", page).build().toUriString();
    }

    /** URL da página atual trocando um parâmetro (valor nulo remove o parâmetro) e voltando à primeira página. */
    public String withParam(String name, Object value) {
        var builder = ServletUriComponentsBuilder.fromCurrentRequest().replaceQueryParam("page");
        return (value == null ? builder.replaceQueryParam(name) : builder.replaceQueryParam(name, value))
                .build().toUriString();
    }
}
