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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    // RedisTemplate opcional - só existe quando profile redis está ativo
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    // Armazena atendimentos ativos (em andamento)
    // Map: ID do atendimento -> Atendimento
    private final Map<Long, Atendimento> atendimentosAtivos = new ConcurrentHashMap<>();

    @Override
    public void distribuir(Atendimento atendimento) {
        if (atendimento == null) {
            log.warn("Tentativa de distribuir atendimento null");
            return;
        }

        log.info("═══════════════════════════════════════");
        log.info("🎯 DISTRIBUINDO ATENDIMENTO");
        log.info("   ID: {}", atendimento.getId());
        log.info("   Cliente: {}", atendimento.getNomeCliente());
        log.info("   Time: {}", atendimento.getTime());
        log.info("═══════════════════════════════════════");

        // Busca atendentes disponíveis do time
        List<Atendente> disponiveis = atendenteService.buscarDisponiveisPorTime(atendimento.getTime());

        // Log de debug para visualizar balanceamento
        if (!disponiveis.isEmpty()) {
            log.debug("Atendentes disponíveis (ordenados por carga): {}",
                disponiveis.stream()
                    .map(a -> String.format("%s(%d/3)", a.getNome(), a.getAtendimentosAtivos()))
                    .collect(Collectors.joining(", ")));
        }

        if (disponiveis.isEmpty()) {
            // Nenhum atendente disponível -> enfileira
            log.warn("⚠️  Nenhum atendente disponível no time {}. Enfileirando atendimento ID {}",
                    atendimento.getTime(), atendimento.getId());

            atendimento.setStatus(StatusAtendimento.AGUARDANDO_ATENDIMENTO);
            filaService.enfileirar(atendimento);

            log.info("📋 Atendimento ID {} adicionado à fila. Tamanho atual da fila: {}",
                    atendimento.getId(), filaService.tamanhoFila(atendimento.getTime()));

            // Notifica dashboard sobre atualização na fila
            notificacaoService.notificarAtualizacaoFila(atendimento.getTime());
        } else {
            // Atendente disponível -> atribui ao primeiro da lista
            log.info("✅ {} atendente(s) disponível(is) no time {}",
                    disponiveis.size(), atendimento.getTime());
            Atendente atendente = disponiveis.get(0);
            atribuirAtendimento(atendimento, atendente);
        }
    }

    @Override
    public void finalizarAtendimento(Long atendimentoId) {
        log.info("═══════════════════════════════════════");
        log.info("🏁 FINALIZANDO ATENDIMENTO");
        log.info("   ID: {}", atendimentoId);

        Atendimento atendimento = atendimentosAtivos.remove(atendimentoId);

        if (atendimento == null) {
            log.warn("⚠️  Tentativa de finalizar atendimento inexistente ou já finalizado: ID {}",
                    atendimentoId);
            log.info("═══════════════════════════════════════");
            return;
        }

        log.info("   Cliente: {}", atendimento.getNomeCliente());
        log.info("   Time: {}", atendimento.getTime());

        // Libera o atendente
        atendenteService.buscarPorId(atendimento.getAtendenteId())
                .ifPresent(atendente -> {
                    atendente.decrementarAtendimento();

                    // Persiste mudança no Redis (se ativo)
                    persistirAtendenteSeRedis(atendente);

                    log.info("   Atendente {} liberado. Atendimentos ativos: {}/3",
                            atendente.getNome(), atendente.getAtendimentosAtivos());
                });

        // Atualiza status do atendimento
        atendimento.setStatus(StatusAtendimento.FINALIZADO);
        atendimento.setDataHoraFinalizacao(LocalDateTime.now());

        // Persiste mudança no Redis (se ativo)
        persistirAtendimentoSeRedis(atendimento);

        log.info("✅ Atendimento finalizado com sucesso");
        log.info("═══════════════════════════════════════");

        // Notifica dashboard
        notificacaoService.notificarAtendimentoFinalizado(atendimento);

        // Processa fila para distribuir próximo atendimento
        processarFila(atendimento.getTime());
    }

    @Override
    public void processarFila(Time time) {
        int tamanhoInicial = filaService.tamanhoFila(time);

        if (tamanhoInicial == 0) {
            log.debug("Fila do time {} está vazia, nada a processar", time);
            return;
        }

        log.info("🔄 Processando fila do time {} (Tamanho: {})", time, tamanhoInicial);

        List<Atendente> disponiveis = atendenteService.buscarDisponiveisPorTime(time);
        int processados = 0;

        while (!disponiveis.isEmpty() && filaService.tamanhoFila(time) > 0) {
            // Remove próximo da fila
            Atendimento proximoAtendimento = filaService.desenfileirar(time);

            if (proximoAtendimento == null) {
                break; // Fila esvaziou
            }

            // Atribui ao primeiro atendente disponível
            Atendente atendente = disponiveis.get(0);
            atribuirAtendimento(proximoAtendimento, atendente);
            processados++;

            // Atualiza lista de disponíveis
            disponiveis = atendenteService.buscarDisponiveisPorTime(time);
        }

        int restante = filaService.tamanhoFila(time);

        log.info("📊 Fila do time {}: {} processado(s), {} restante(s)",
                time, processados, restante);

        if (restante > 0) {
            log.warn("⚠️  Ainda há {} atendimento(s) aguardando no time {}",
                    restante, time);
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
        atendente.incrementarAtendimento();

        // Persiste mudança no Redis (se ativo)
        persistirAtendenteSeRedis(atendente);
        persistirAtendimentoSeRedis(atendimento);

        // Armazena em memória como ativo
        atendimentosAtivos.put(atendimento.getId(), atendimento);

        log.info("👤 Atendimento {} atribuído para {} (Time: {}). Carga: {}/3 (Least Connection)",
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

    /**
     * Persiste mudanças do atendente no Redis (se profile redis ativo).
     * Quando usando Redis, as mudanças no objeto Atendente em memória
     * precisam ser sincronizadas com o Redis.
     */
    private void persistirAtendenteSeRedis(Atendente atendente) {
        if (redisTemplate != null) {
            String key = "atendente:" + atendente.getId();
            redisTemplate.opsForHash().put(key, "atendimentosAtivos",
                    atendente.getAtendimentosAtivos());
            log.debug("Atendente {} atualizado no Redis: {}/3 atendimentos",
                    atendente.getId(), atendente.getAtendimentosAtivos());
        }
    }

    /**
     * Persiste mudanças do atendimento no Redis (se profile redis ativo).
     * Quando usando Redis, as mudanças no objeto Atendimento em memória
     * precisam ser sincronizadas com o Redis.
     */
    private void persistirAtendimentoSeRedis(Atendimento atendimento) {
        if (redisTemplate != null) {
            String key = "atendimento:" + atendimento.getId();
            redisTemplate.opsForValue().set(key, atendimento);
            log.debug("Atendimento {} atualizado no Redis: status={}",
                    atendimento.getId(), atendimento.getStatus());
        }
    }
}
