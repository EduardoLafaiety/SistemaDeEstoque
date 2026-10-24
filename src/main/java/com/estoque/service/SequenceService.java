package com.estoque.service;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import com.estoque.domain.PickingType;

/** Gera referências legíveis (IN/00001, P00001, MO/00001) a partir de sequências do PostgreSQL. */
@Service
public class SequenceService {

    private final JdbcClient jdbc;

    public SequenceService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public String nextPicking(PickingType type) {
        String sequence = switch (type) {
            case RECEIPT -> "seq_picking_in";
            case DELIVERY -> "seq_picking_out";
            case INTERNAL -> "seq_picking_int";
        };
        return type.getPrefix() + "/" + next(sequence);
    }

    public String nextPurchase() {
        return "P" + next("seq_purchase");
    }

    public String nextProduction() {
        return "MO/" + next("seq_production");
    }

    /** {@code sequence} vem sempre de constantes desta classe, nunca de entrada do usuário. */
    private String next(String sequence) {
        long value = jdbc.sql("select nextval('" + sequence + "')").query(Long.class).single();
        return "%05d".formatted(value);
    }
}
