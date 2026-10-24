package com.estoque.service;

import com.estoque.domain.ReorderTrigger;

/**
 * Filtros da tela de reposição.
 *
 * @param locationId  limita à subárvore do local (ou {@code null} para todos)
 * @param trigger     gatilho (ou {@code null} para todos)
 * @param term        busca por nome/código do produto
 * @param toOrderOnly apenas linhas com quantidade a pedir
 * @param snoozed     mostra as regras adiadas em vez das ativas
 */
public record ReplenishmentFilter(Long locationId, ReorderTrigger trigger, String term, boolean toOrderOnly,
                                  boolean snoozed) {
}
