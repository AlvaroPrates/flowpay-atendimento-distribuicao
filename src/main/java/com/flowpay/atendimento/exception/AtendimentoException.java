package com.flowpay.atendimento.exception;

/**
 * Exceção base para erros de negócio do sistema.
 */
public class AtendimentoException extends RuntimeException {

    public AtendimentoException(String message) {
        super(message);
    }

    public AtendimentoException(String message, Throwable cause) {
        super(message, cause);
    }
}
