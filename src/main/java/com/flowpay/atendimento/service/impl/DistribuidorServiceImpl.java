package com.flowpay.atendimento.service.impl;

import com.flowpay.atendimento.model.Atendimento;
import com.flowpay.atendimento.model.Atendente;
import com.flowpay.atendimento.model.StatusAtendimento;
import com.flowpay.atendimento.model.Time;
import com.flowpay.atendimento.service.AtendenteService;
import com.flowpay.atendimento.service.DistribuidorService;
import com.flowpay.atendimento.service.FilaService;
import com.flowpay.atendimento.service.NotificacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementação do serviço de distribuição de atendimentos.
 *
 * Lógica principal:
 * 1. Recebe atendimento -> tenta distribuir para atendente disponível
 * 2. Se não houver disponível -> enfileira
 * 3. Quando atendimento finaliza -> processa fila para distribuir próximo
 *
 * Esta implementação é agnóstica de storage (funciona com Memory, Redis, etc.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistribuidorServiceImpl implements DistribuidorService {

    private final FilaService filaService;
    private final AtendenteService atendenteService;
    private final NotificacaoService notificacaoService;

    // Armazena atendimentos ativos (em andamento)
    // Map: ID do atendimento -> Atendimento
    private final Map<Long, Atendimento> atendimentosAtivos = new ConcurrentHashMap<>();

    @Override
    public void distribuir(Atendimento atendimento) {
        if (atendimento == null) {
            log.warn("Tentativa de distribuir atendimento null");
            return;
        }

        log.info("Iniciando distribuição do atendimento ID {} para time {}",
                atendimento.getId(), atendimento.getTime());

        // Busca atendentes disponíveis do time
        List<Atendente> disponiveis = atendenteService.buscarDisponiveis(atendimento.getTime());

        if (disponiveis.isEmpty()) {
            // Nenhum atendente disponível -> enfileira
            log.info("Nenhum atendente disponível no time {}. Enfileirando atendimento ID {}",
                    atendimento.getTime(), atendimento.getId());

            atendimento.setStatus(StatusAtendimento.AGUARDANDO_ATENDIMENTO);
            filaService.enfileirar(atendimento);

            // Notifica dashboard sobre atualização na fila
            notificacaoService.notificarAtualizacaoFila(atendimento.getTime());
        } else {
            // Atendente disponível -> atribui ao primeiro da lista
            Atendente atendente = disponiveis.get(0);
            atribuirAtendimento(atendimento, atendente);
        }
    }

    @Override
    public void finalizarAtendimento(Long atendimentoId) {
        log.info("Finalizando atendimento ID {}", atendimentoId);

        Atendimento atendimento = atendimentosAtivos.remove(atendimentoId);

        if (atendimento == null) {
            log.warn("Tentativa de finalizar atendimento inexistente ou já finalizado: ID {}",
                    atendimentoId);
            return;
        }

        // Libera o atendente
        atendenteService.buscarPorId(atendimento.getAtendenteId())
                .ifPresent(atendente -> {
                    atendente.decrementarAtendimentos();
                    log.info("Atendente {} liberado. Atendimentos ativos: {}/3",
                            atendente.getNome(), atendente.getAtendimentosAtivos());
                });

        // Atualiza status do atendimento
        atendimento.setStatus(StatusAtendimento.FINALIZADO);
        atendimento.setDataHoraFinalizacao(LocalDateTime.now());

        log.info("Atendimento ID {} finalizado com sucesso", atendimentoId);

        // Notifica dashboard
        notificacaoService.notificarAtendimentoFinalizado(atendimento);

        // Processa fila para distribuir próximo atendimento
        processarFila(atendimento.getTime());
    }

    @Override
    public void processarFila(Time time) {
        log.debug("Processando fila do time {}", time);

        // Enquanto houver atendentes disponíveis E fila não vazia
        List<Atendente> disponiveis = atendenteService.buscarDisponiveis(time);

        while (!disponiveis.isEmpty() && filaService.tamanhoFila(time) > 0) {
            // Remove próximo da fila
            Atendimento proximoAtendimento = filaService.desenfileirar(time);

            if (proximoAtendimento == null) {
                break; // Fila esvaziou
            }

            // Atribui ao primeiro atendente disponível
            Atendente atendente = disponiveis.get(0);
            atribuirAtendimento(proximoAtendimento, atendente);

            // Atualiza lista de disponíveis
            disponiveis = atendenteService.buscarDisponiveis(time);
        }

        int restante = filaService.tamanhoFila(time);
        if (restante > 0) {
            log.debug("Processamento da fila do time {} concluído. Ainda {} na fila",
                    time, restante);
        } else {
            log.debug("Fila do time {} completamente processada", time);
        }
    }

    /**
     * Atribui um atendimento a um atendente específico.
     * Método privado auxiliar para evitar duplicação de código.
     */
    private void atribuirAtendimento(Atendimento atendimento, Atendente atendente) {
        // Atualiza dados do atendimento
        atendimento.setAtendenteId(atendente.getId());
        atendimento.setStatus(StatusAtendimento.EM_ATENDIMENTO);
        atendimento.setDataHoraAtendimento(LocalDateTime.now());

        // Incrementa contador do atendente
        atendente.incrementarAtendimentos();

        // Armazena em memória como ativo
        atendimentosAtivos.put(atendimento.getId(), atendimento);

        log.info("Atendimento {} atribuído para {} (Time: {}). Carga atual: {}/3",
                atendimento.getId(),
                atendente.getNome(),
                atendente.getTime(),
                atendente.getAtendimentosAtivos());

        // Notifica dashboard
        notificacaoService.notificarNovoAtendimento(atendimento);
    }

    /**
     * Método auxiliar para obter todos os atendimentos ativos.
     * Útil para debugging e dashboard.
     */
    public List<Atendimento> listarAtendimentosAtivos() {
        return List.copyOf(atendimentosAtivos.values());
    }

    /**
     * Método auxiliar para obter atendimentos ativos de um time específico.
     */
    public List<Atendimento> listarAtendimentosAtivosPorTime(Time time) {
        return atendimentosAtivos.values().stream()
                .filter(a -> a.getTime() == time)
                .toList();
    }
}
