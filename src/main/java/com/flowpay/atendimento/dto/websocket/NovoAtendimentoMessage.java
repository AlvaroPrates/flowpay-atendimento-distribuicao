package com.flowpay.atendimento.dto.websocket;

import com.flowpay.atendimento.model.Time;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dados enviados quando um novo atendimento é criado/atribuído.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NovoAtendimentoMessage {

    private Long atendimentoId;
    private String nomeCliente;
    private String assunto;
    private Time time;
    private Long atendenteId;
    private String nomeAtendente;
}
