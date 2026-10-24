package com.estoque.service;

/** Violação de regra de negócio, exibida ao usuário como mensagem de erro. */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public static BusinessException notFound(String what) {
        return new BusinessException(what + " não encontrado(a).");
    }
}
