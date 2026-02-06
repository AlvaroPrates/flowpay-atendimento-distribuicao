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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class NotificacaoServiceImpl implements NotificacaoService {

    private final SimpMessagingTemplate messagingTemplate;
    private final AtendenteService atendenteService;
    private final AtendimentoService atendimentoService;
    private final FilaService filaService;

    public NotificacaoServiceImpl(
            SimpMessagingTemplate messagingTemplate,
            AtendenteService atendenteService,
            @Lazy AtendimentoService atendimentoService,
            FilaService filaService) {
        this.messagingTemplate = messagingTemplate;
        this.atendenteService = atendenteService;
        this.atendimentoService = atendimentoService;
        this.filaService = filaService;
    }

    @Override
    public void notificarNovoAtendimento(Atendimento atendimento) {
        log.info("[NOTIFICAÇÃO WS] Novo atendimento: ID={}, Cliente={}, Atendente={}",
                atendimento.getId(),
                atendimento.getNomeCliente(),
                atendimento.getAtendenteId());

        String nomeAtendente = null;

        if (atendimento.getAtendenteId() != null) {
            nomeAtendente = atendenteService.buscarPorId(atendimento.getAtendenteId())
                    .map(Atendente::getNome)
                    .orElse("Desconhecido");
        }

        NovoAtendimentoMessage dados = NovoAtendimentoMessage.builder()
                .atendimentoId(atendimento.getId())
                .nomeCliente(atendimento.getNomeCliente())
                .assunto(atendimento.getAssunto())
                .time(atendimento.getTime())
                .atendenteId(atendimento.getAtendenteId())
                .nomeAtendente(nomeAtendente)
                .build();

        WebSocketMessage mensagem = WebSocketMessage.builder()
                .tipo(WebSocketMessage.TipoMensagem.NOVO_ATENDIMENTO)
                .timestamp(LocalDateTime.now())
                .dados(dados)
                .mensagem("Novo atendimento em andamento")
                .build();

        messagingTemplate.convertAndSend("/topic/atendimentos", mensagem);

        notificarMetricasAtualizadas();
    }

    @Override
    public void notificarAtualizacaoFila(Time time) {
        log.info("[NOTIFICAÇÃO WS] Fila atualizada: Time={}", time);

        int ativos = (int) atendimentoService.listarPorTime(time)
                .stream()
                .filter(a -> a.getStatus() == StatusAtendimento.EM_ATENDIMENTO)
                .count();

        FilaAtualizadaMessage dados = FilaAtualizadaMessage.builder()
                .time(time)
                .tamanhoFila(filaService.tamanhoFila(time))
                .atendimentosAtivos(ativos)
                .build();

        WebSocketMessage mensagem = WebSocketMessage.builder()
                .tipo(WebSocketMessage.TipoMensagem.FILA_ATUALIZADA)
                .timestamp(LocalDateTime.now())
                .dados(dados)
                .mensagem("Fila do time " + time + " foi atualizada")
                .build();

        messagingTemplate.convertAndSend("/topic/fila/" + time.name(), mensagem);
        messagingTemplate.convertAndSend("/topic/filas", mensagem);

        notificarMetricasAtualizadas();
    }

    @Override
    public void notificarAtendimentoFinalizado(Atendimento atendimento) {
        log.info("[NOTIFICAÇÃO WS] Atendimento finalizado: ID={}, Cliente={}",
                atendimento.getId(),
                atendimento.getNomeCliente());

        WebSocketMessage mensagem = WebSocketMessage.builder()
                .tipo(WebSocketMessage.TipoMensagem.ATENDIMENTO_FINALIZADO)
                .timestamp(LocalDateTime.now())
                .dados(atendimento.getId())
                .mensagem("Atendimento " + atendimento.getId() + " foi finalizado")
                .build();

        messagingTemplate.convertAndSend("/topic/atendimentos", mensagem);

        notificarMetricasAtualizadas();
    }

    @Override
    public void notificarNovoAtendente(Atendente atendente) {
        log.info("[NOTIFICAÇÃO WS] Novo atendente cadastrado: ID={}, Nome={}, Time={}",
                atendente.getId(),
                atendente.getNome(),
                atendente.getTime());

        NovoAtendenteMessage dados = NovoAtendenteMessage.builder()
                .atendenteId(atendente.getId())
                .nome(atendente.getNome())
                .time(atendente.getTime())
                .atendimentosAtivos(atendente.getAtendimentosAtivos())
                .build();

        WebSocketMessage mensagem = WebSocketMessage.builder()
                .tipo(WebSocketMessage.TipoMensagem.NOVO_ATENDENTE)
                .timestamp(LocalDateTime.now())
                .dados(dados)
                .mensagem("Novo atendente cadastrado no time " + atendente.getTime())
                .build();

        messagingTemplate.convertAndSend("/topic/atendentes/" + atendente.getTime().name(), mensagem);
        messagingTemplate.convertAndSend("/topic/atendentes", mensagem);

        notificarMetricasAtualizadas();
    }

    private void notificarMetricasAtualizadas() {
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

        MetricasAtualizadasMessage dados = MetricasAtualizadasMessage.builder()
                .totalAtendimentosAtivos(totalAtivos)
                .totalNaFila(totalFila)
                .totalAtendentes(totalAtendentes)
                .atendentesDisponiveis(disponiveis)
                .build();

        WebSocketMessage mensagem = WebSocketMessage.builder()
                .tipo(WebSocketMessage.TipoMensagem.METRICAS_ATUALIZADAS)
                .timestamp(LocalDateTime.now())
                .dados(dados)
                .mensagem("Métricas do sistema atualizadas")
                .build();

        messagingTemplate.convertAndSend("/topic/metricas", mensagem);

        log.debug("[NOTIFICAÇÃO WS] Métricas atualizadas: ativos={}, fila={}, atendentes={}/{}",
                totalAtivos, totalFila, disponiveis, totalAtendentes);
    }
}
