package com.flowpay.atendimento.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Atendente {
    public static final int MAX_ATENDIMENTOS = 3;

    private Long id;
    private String nome;
    private Time time;
    private int atendimentosAtivos;

    public boolean isDisponivel() {
        return atendimentosAtivos < MAX_ATENDIMENTOS;
    }

    public boolean podeAtender() {
        return isDisponivel();
    }

    public void incrementarAtendimentos() {
        if (atendimentosAtivos < MAX_ATENDIMENTOS) {
            atendimentosAtivos++;
        }
    }

    public void decrementarAtendimentos() {
        if (atendimentosAtivos > 0) {
            atendimentosAtivos--;
        }
    }
}
