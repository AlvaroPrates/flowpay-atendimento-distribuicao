package com.flowpay.atendimento.dto.websocket;

import com.flowpay.atendimento.model.Time;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dados enviados quando uma fila é atualizada.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilaAtualizadaMessage {

    private Time time;
    private int tamanhoFila;
    private int atendimentosAtivos;
}
