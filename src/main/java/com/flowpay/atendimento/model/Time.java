package com.flowpay.atendimento.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Time {
    CARTOES("Problemas com cartão"),
    EMPRESTIMOS("Contratação de empréstimo"),
    OUTROS("Outros Assuntos");

    private final String descricao;
}
