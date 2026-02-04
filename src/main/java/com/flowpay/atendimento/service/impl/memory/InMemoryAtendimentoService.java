package com.flowpay.atendimento.service.impl.memory;

import com.flowpay.atendimento.model.Atendimento;
import com.flowpay.atendimento.model.StatusAtendimento;
import com.flowpay.atendimento.model.Time;
import com.flowpay.atendimento.service.AtendimentoService;
import com.flowpay.atendimento.service.DistribuidorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Implementação em memória do serviço de atendimentos.
 * Gerencia o CRUD de atendimentos.
 */
@Service
@Profile("memory")
@RequiredArgsConstructor
@Slf4j
public class InMemoryAtendimentoService implements AtendimentoService {

    private final DistribuidorService distribuidorService;

    // Map: ID -> Atendimento
    private final Map<Long, Atendimento> atendimentos = new ConcurrentHashMap<>();

    // Gerador de IDs
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Atendimento criar(Atendimento atendimento) {
        if (atendimento == null) {
            throw new IllegalArgumentException("Atendimento não pode ser null");
        }

        // Gera ID
        atendimento.setId(idGenerator.getAndIncrement());

        // Define status inicial
        atendimento.setStatus(StatusAtendimento.AGUARDANDO_ATENDIMENTO);

        // Define data de criação
        atendimento.setDataHoraCriacao(LocalDateTime.now());

        // Salva
        atendimentos.put(atendimento.getId(), atendimento);

        log.info("Atendimento criado: ID={}, Cliente={}, Assunto={}, Time={}",
                atendimento.getId(),
                atendimento.getNomeCliente(),
                atendimento.getAssunto(),
                atendimento.getTime());

        // Distribui automaticamente
        distribuidorService.distribuir(atendimento);

        return atendimento;
    }

    @Override
    public Optional<Atendimento> buscarPorId(Long id) {
        return Optional.ofNullable(atendimentos.get(id));
    }

    @Override
    public List<Atendimento> listarPorTime(Time time) {
        return atendimentos.values().stream()
                .filter(a -> a.getTime() == time)
                .collect(Collectors.toList());
    }

    @Override
    public List<Atendimento> listarPorStatus(StatusAtendimento status) {
        return atendimentos.values().stream()
                .filter(a -> a.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public List<Atendimento> listarTodos() {
        return new ArrayList<>(atendimentos.values());
    }
}
