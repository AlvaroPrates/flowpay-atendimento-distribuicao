package com.flowpay.atendimento.exception;

/**
 * Exceção lançada quando um recurso não é encontrado.
 */
public class RecursoNaoEncontradoException extends AtendimentoException {

    public RecursoNaoEncontradoException(String message) {
        super(message);
    }
}
