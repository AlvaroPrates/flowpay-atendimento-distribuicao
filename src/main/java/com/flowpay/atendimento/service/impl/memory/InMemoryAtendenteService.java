package com.flowpay.atendimento.service.impl.memory;

import com.flowpay.atendimento.model.Atendente;
import com.flowpay.atendimento.model.Time;
import com.flowpay.atendimento.service.AtendenteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Implementação em memória do serviço de atendentes.
 * Usa ConcurrentHashMap para armazenamento thread-safe.
 * Ativa apenas quando profile "memory" está ativo.
 */
@Service
@Profile("memory")
@Slf4j
public class InMemoryAtendenteService implements AtendenteService {

    // Map: ID -> Atendente
    private final Map<Long, Atendente> atendentes = new ConcurrentHashMap<>();

    // Gerador de IDs auto-incrementais
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Atendente cadastrar(Atendente atendente) {
        if (atendente == null) {
            throw new IllegalArgumentException("Atendente não pode ser null");
        }

        // Gera ID se não tiver
        if (atendente.getId() == null) {
            atendente.setId(idGenerator.getAndIncrement());
        }

        // Inicializa atendimentos ativos em 0 se não definido
        if (atendente.getAtendimentosAtivos() == 0) {
            atendente.setAtendimentosAtivos(0);
        }

        atendentes.put(atendente.getId(), atendente);

        log.info("Atendente cadastrado: ID={}, Nome={}, Time={}",
                atendente.getId(), atendente.getNome(), atendente.getTime());

        return atendente;
    }

    @Override
    public List<Atendente> buscarDisponiveis(Time time) {
        List<Atendente> disponiveis = atendentes.values().stream()
                .filter(a -> a.getTime() == time)
                .filter(Atendente::isDisponivel)
                .collect(Collectors.toList());

        log.debug("Time {}: {} atendentes disponíveis de {} totais",
                time, disponiveis.size(),
                atendentes.values().stream().filter(a -> a.getTime() == time).count());

        return disponiveis;
    }

    @Override
    public Optional<Atendente> buscarPorId(Long id) {
        return Optional.ofNullable(atendentes.get(id));
    }

    @Override
    public List<Atendente> listarPorTime(Time time) {
        return atendentes.values().stream()
                .filter(a -> a.getTime() == time)
                .collect(Collectors.toList());
    }

    @Override
    public List<Atendente> listarTodos() {
        return new ArrayList<>(atendentes.values());
    }
}
