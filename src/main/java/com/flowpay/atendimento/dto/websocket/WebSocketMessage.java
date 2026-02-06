package com.flowpay.atendimento.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    private TipoMensagem tipo;

    private LocalDateTime timestamp;

    private Object dados;

    private String mensagem;

    public enum TipoMensagem {
        NOVO_ATENDIMENTO,
        ATENDIMENTO_FINALIZADO,
        FILA_ATUALIZADA,
        METRICAS_ATUALIZADAS,
        ATENDENTE_STATUS_ALTERADO,
        NOVO_ATENDENTE
    }
}
