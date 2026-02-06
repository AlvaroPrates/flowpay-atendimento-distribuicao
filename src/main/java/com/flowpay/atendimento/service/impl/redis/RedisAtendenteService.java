package com.flowpay.atendimento.service.impl.redis;

import com.flowpay.atendimento.model.Atendente;
import com.flowpay.atendimento.model.Time;
import com.flowpay.atendimento.service.AtendenteService;
import com.flowpay.atendimento.service.DistribuidorService;
import com.flowpay.atendimento.service.NotificacaoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Profile("redis")
@Slf4j
public class RedisAtendenteService implements AtendenteService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificacaoService notificacaoService;
    private final DistribuidorService distribuidorService;

    public RedisAtendenteService(
            RedisTemplate<String, Object> redisTemplate,
            @Lazy NotificacaoService notificacaoService,
            @Lazy DistribuidorService distribuidorService) {
        this.redisTemplate = redisTemplate;
        this.notificacaoService = notificacaoService;
        this.distribuidorService = distribuidorService;
    }

    private static final String ATENDENTE_PREFIX = "atendente:";
    private static final String ATENDENTES_IDS_KEY = "atendentes:ids";
    private static final String ID_COUNTER_KEY = "atendente:id:counter";

    private String getAtendenteKey(Long id) {
        return ATENDENTE_PREFIX + id;
    }

    private Long gerarProximoId() {
        return redisTemplate.opsForValue().increment(ID_COUNTER_KEY);
    }

    @Override
    public Atendente cadastrar(Atendente atendente) {
        if (atendente == null) {
            throw new IllegalArgumentException("Atendente não pode ser null");
        }

        var atendenteId = atendente.getId();

        if (atendenteId == null || atendenteId == 0) {
            atendente.setId(gerarProximoId());
        }

        atendente.setAtendimentosAtivos(0);

        String key = getAtendenteKey(atendente.getId());

        redisTemplate.opsForHash().put(key, "id", atendente.getId());
        redisTemplate.opsForHash().put(key, "nome", atendente.getNome());
        redisTemplate.opsForHash().put(key, "time", atendente.getTime().name());
        redisTemplate.opsForHash().put(key, "atendimentosAtivos", atendente.getAtendimentosAtivos());

        redisTemplate.opsForSet().add(ATENDENTES_IDS_KEY, atendente.getId());

        log.info("Atendente cadastrado no Redis: ID={}, Nome={}, Time={}",
                atendente.getId(), atendente.getNome(), atendente.getTime());

        notificacaoService.notificarNovoAtendente(atendente);

        // Processa fila do time para distribuir atendimentos pendentes
        distribuidorService.processarFila(atendente.getTime());

        return atendente;
    }

    @Override
    public List<Atendente> buscarDisponiveisPorTime(Time time) {
        return listarPorTime(time).stream()
                .filter(Atendente::isDisponivel)
                .sorted(Comparator.comparingInt(Atendente::getAtendimentosAtivos))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Atendente> buscarPorId(Long id) {
        String key = getAtendenteKey(id);

        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            return Optional.empty();
        }

        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        if (entries.isEmpty()) {
            return Optional.empty();
        }

        Atendente atendente = Atendente.builder()
                .id(((Number) entries.get("id")).longValue())
                .nome((String) entries.get("nome"))
                .time(Time.valueOf((String) entries.get("time")))
                .atendimentosAtivos(((Number) entries.get("atendimentosAtivos")).intValue())
                .build();

        return Optional.of(atendente);
    }

    @Override
    public List<Atendente> listarPorTime(Time time) {
        return listarTodos().stream()
                .filter(a -> a.getTime() == time)
                .collect(Collectors.toList());
    }

    @Override
    public List<Atendente> listarTodos() {
        Set<Object> ids = redisTemplate.opsForSet().members(ATENDENTES_IDS_KEY);

        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        List<Atendente> atendentes = new ArrayList<>();
        for (Object idObj : ids) {
            Long id = ((Number) idObj).longValue();
            buscarPorId(id).ifPresent(atendentes::add);
        }

        return atendentes;
    }
}
