package com.flowpay.atendimento.service.impl;

import com.flowpay.atendimento.model.Atendimento;
import com.flowpay.atendimento.model.Time;
import com.flowpay.atendimento.service.NotificacaoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementação básica do serviço de notificações.
 * Por enquanto apenas loga as notificações.
 * Será expandido para usar WebSocket posteriormente.
 */
@Service
@Slf4j
public class NotificacaoServiceImpl implements NotificacaoService {

    @Override
    public void notificarNovoAtendimento(Atendimento atendimento) {
        log.info("[NOTIFICAÇÃO] Novo atendimento em andamento: ID={}, Cliente={}, Atendente={}",
                atendimento.getId(),
                atendimento.getNomeCliente(),
                atendimento.getAtendenteId());

        // TODO: Implementar envio via WebSocket
    }

    @Override
    public void notificarAtualizacaoFila(Time time) {
        log.info("[NOTIFICAÇÃO] Fila do time {} foi atualizada", time);

        // TODO: Implementar envio via WebSocket
    }

    @Override
    public void notificarAtendimentoFinalizado(Atendimento atendimento) {
        log.info("[NOTIFICAÇÃO] Atendimento finalizado: ID={}, Cliente={}",
                atendimento.getId(),
                atendimento.getNomeCliente());

        // TODO: Implementar envio via WebSocket
    }
}
