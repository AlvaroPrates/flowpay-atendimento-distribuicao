package com.flowpay.atendimento.service.impl.memory;

import com.flowpay.atendimento.model.Atendente;
import com.flowpay.atendimento.model.Time;
import com.flowpay.atendimento.service.AtendenteService;
import com.flowpay.atendimento.service.DistribuidorService;
import com.flowpay.atendimento.service.NotificacaoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
@Profile("memory")
@Slf4j
public class InMemoryAtendenteService implements AtendenteService {

    private final Map<Long, Atendente> atendentes = new ConcurrentHashMap<>();

    private final AtomicLong idGenerator = new AtomicLong(1);

    private final NotificacaoService notificacaoService;
    private final DistribuidorService distribuidorService;

    public InMemoryAtendenteService(
            @Lazy NotificacaoService notificacaoService,
            @Lazy DistribuidorService distribuidorService) {
        this.notificacaoService = notificacaoService;
        this.distribuidorService = distribuidorService;
    }

    @Override
    public Atendente cadastrar(Atendente atendente) {
        if (atendente == null) {
            throw new IllegalArgumentException("Atendente não pode ser null");
        }

        var atendenteId = atendente.getId();

        if (atendenteId == null || atendenteId == 0) {
            atendente.setId(idGenerator.getAndIncrement());
        }

        atendente.setAtendimentosAtivos(0);

        atendentes.put(atendente.getId(), atendente);

        log.info("Atendente cadastrado: ID={}, Nome={}, Time={}",
                atendente.getId(), atendente.getNome(), atendente.getTime());

        notificacaoService.notificarNovoAtendente(atendente);

        // Processa fila do time para distribuir atendimentos pendentes
        distribuidorService.processarFila(atendente.getTime());

        return atendente;
    }

    @Override
    public List<Atendente> buscarDisponiveisPorTime(Time time) {
        List<Atendente> disponiveis = atendentes.values().stream()
                .filter(a -> a.getTime() == time)
                .filter(Atendente::isDisponivel)
                .sorted(Comparator.comparingInt(Atendente::getAtendimentosAtivos))
                .collect(Collectors.toList());

        log.debug("Time {}: {} atendentes disponíveis de {} totais (ordenados por carga)",
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
