package com.flowpay.atendimento.dto.response;

import com.flowpay.atendimento.model.Time;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO com status completo de um time.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeStatusResponse {

    private Time time;
    private int tamanhoFila;
    private int atendimentosAtivos;
    private List<AtendenteResponse> atendentes;
    private List<AtendimentoResponse> fila;
}
