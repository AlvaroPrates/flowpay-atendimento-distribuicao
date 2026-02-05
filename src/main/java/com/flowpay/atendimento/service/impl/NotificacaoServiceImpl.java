package com.flowpay.atendimento.service.impl;

import com.flowpay.atendimento.dto.websocket.*;
import com.flowpay.atendimento.model.Atendimento;
import com.flowpay.atendimento.model.Atendente;
import com.flowpay.atendimento.model.StatusAtendimento;
import com.flowpay.atendimento.model.Time;
import com.flowpay.atendimento.service.AtendenteService;
import com.flowpay.atendimento.service.AtendimentoService;
import com.flowpay.atendimento.service.FilaService;
import com.flowpay.atendimento.service.NotificacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Implementação do serviço de notificações via WebSocket.
 * Envia mensagens em tempo real para clientes conectados.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacaoServiceImpl implements NotificacaoService {

    private final SimpMessagingTemplate messagingTemplate;
    private final AtendenteService atendenteService;
    private final AtendimentoService atendimentoService;
    private final FilaService filaService;

    @Override
    public void notificarNovoAtendimento(Atendimento atendimento) {
        log.info("[NOTIFICAÇÃO WS] Novo atendimento: ID={}, Cliente={}, Atendente={}",
                atendimento.getId(),
                atendimento.getNomeCliente(),
                atendimento.getAtendenteId());

        // Busca nome do atendente
        String nomeAtendente = null;
        if (atendimento.getAtendenteId() != null) {
            nomeAtendente = atendenteService.buscarPorId(atendimento.getAtendenteId())
                    .map(Atendente::getNome)
                    .orElse("Desconhecido");
        }

        // Monta dados do atendimento
        NovoAtendimentoMessage dados = NovoAtendimentoMessage.builder()
                .atendimentoId(atendimento.getId())
                .nomeCliente(atendimento.getNomeCliente())
                .assunto(atendimento.getAssunto())
                .time(atendimento.getTime())
                .atendenteId(atendimento.getAtendenteId())
                .nomeAtendente(nomeAtendente)
                .build();

        // Monta mensagem WebSocket
        WebSocketMessage mensagem = WebSocketMessage.builder()
                .tipo(WebSocketMessage.TipoMensagem.NOVO_ATENDIMENTO)
                .timestamp(LocalDateTime.now())
                .dados(dados)
                .mensagem("Novo atendimento em andamento")
                .build();

        // Envia para todos os clientes inscritos em /topic/atendimentos
        messagingTemplate.convertAndSend("/topic/atendimentos", mensagem);

        // Também notifica métricas atualizadas
        notificarMetricasAtualizadas();
    }

    @Override
    public void notificarAtualizacaoFila(Time time) {
        log.info("[NOTIFICAÇÃO WS] Fila atualizada: Time={}", time);

        // Conta atendimentos ativos deste time
        int ativos = (int) atendimentoService.listarPorTime(time)
                .stream()
                .filter(a -> a.getStatus() == StatusAtendimento.EM_ATENDIMENTO)
                .count();

        // Monta dados da fila
        FilaAtualizadaMessage dados = FilaAtualizadaMessage.builder()
                .time(time)
                .tamanhoFila(filaService.tamanhoFila(time))
                .atendimentosAtivos(ativos)
                .build();

        // Monta mensagem WebSocket
        WebSocketMessage mensagem = WebSocketMessage.builder()
                .tipo(WebSocketMessage.TipoMensagem.FILA_ATUALIZADA)
                .timestamp(LocalDateTime.now())
                .dados(dados)
                .mensagem("Fila do time " + time + " foi atualizada")
                .build();

        // Envia para tópico específico do time
        messagingTemplate.convertAndSend("/topic/fila/" + time.name(), mensagem);

        // Também envia para tópico geral
        messagingTemplate.convertAndSend("/topic/filas", mensagem);

        // Atualiza métricas gerais
        notificarMetricasAtualizadas();
    }

    @Override
    public void notificarAtendimentoFinalizado(Atendimento atendimento) {
        log.info("[NOTIFICAÇÃO WS] Atendimento finalizado: ID={}, Cliente={}",
                atendimento.getId(),
                atendimento.getNomeCliente());

        // Monta mensagem WebSocket
        WebSocketMessage mensagem = WebSocketMessage.builder()
                .tipo(WebSocketMessage.TipoMensagem.ATENDIMENTO_FINALIZADO)
                .timestamp(LocalDateTime.now())
                .dados(atendimento.getId())
                .mensagem("Atendimento " + atendimento.getId() + " foi finalizado")
                .build();

        // Envia para todos os clientes
        messagingTemplate.convertAndSend("/topic/atendimentos", mensagem);

        // Atualiza métricas
        notificarMetricasAtualizadas();
    }

    /**
     * Notifica que as métricas gerais do sistema foram atualizadas.
     */
    private void notificarMetricasAtualizadas() {
        // Calcula métricas atuais
        int totalAtivos = atendimentoService.listarPorStatus(StatusAtendimento.EM_ATENDIMENTO)
                .size();

        int totalFila = Arrays.stream(Time.values())
                .mapToInt(filaService::tamanhoFila)
                .sum();

        List<Atendente> todosAtendentes = atendenteService.listarTodos();
        int totalAtendentes = todosAtendentes.size();
        int disponiveis = (int) todosAtendentes.stream()
                .filter(Atendente::isDisponivel)
                .count();

        // Monta dados
        MetricasAtualizadasMessage dados = MetricasAtualizadasMessage.builder()
                .totalAtendimentosAtivos(totalAtivos)
                .totalNaFila(totalFila)
                .totalAtendentes(totalAtendentes)
                .atendentesDisponiveis(disponiveis)
                .build();

        // Monta mensagem
        WebSocketMessage mensagem = WebSocketMessage.builder()
                .tipo(WebSocketMessage.TipoMensagem.METRICAS_ATUALIZADAS)
                .timestamp(LocalDateTime.now())
                .dados(dados)
                .mensagem("Métricas do sistema atualizadas")
                .build();

        // Envia para tópico de métricas
        messagingTemplate.convertAndSend("/topic/metricas", mensagem);

        log.debug("[NOTIFICAÇÃO WS] Métricas atualizadas: ativos={}, fila={}, atendentes={}/{}",
                totalAtivos, totalFila, disponiveis, totalAtendentes);
    }
}
