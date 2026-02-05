package com.flowpay.atendimento.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Estrutura base para mensagens WebSocket.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    /**
     * Tipo da mensagem/evento
     */
    private TipoMensagem tipo;

    /**
     * Timestamp da mensagem
     */
    private LocalDateTime timestamp;

    /**
     * Dados da mensagem (pode ser qualquer objeto)
     */
    private Object dados;

    /**
     * Mensagem descritiva (opcional)
     */
    private String mensagem;

    public enum TipoMensagem {
        NOVO_ATENDIMENTO,
        ATENDIMENTO_FINALIZADO,
        FILA_ATUALIZADA,
        METRICAS_ATUALIZADAS,
        ATENDENTE_STATUS_ALTERADO
    }
}
