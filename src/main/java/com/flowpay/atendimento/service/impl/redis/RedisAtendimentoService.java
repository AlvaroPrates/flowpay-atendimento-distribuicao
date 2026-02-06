package com.flowpay.atendimento.service.impl.redis;

import com.flowpay.atendimento.model.Atendimento;
import com.flowpay.atendimento.model.StatusAtendimento;
import com.flowpay.atendimento.model.Time;
import com.flowpay.atendimento.service.AtendimentoService;
import com.flowpay.atendimento.service.DistribuidorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Profile("redis")
@RequiredArgsConstructor
@Slf4j
public class RedisAtendimentoService implements AtendimentoService {

    private final DistribuidorService distribuidorService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ATENDIMENTO_PREFIX = "atendimento:";
    private static final String ATENDIMENTOS_IDS_KEY = "atendimentos:ids";
    private static final String ID_COUNTER_KEY = "atendimento:id:counter";

    private String getAtendimentoKey(Long id) {
        return ATENDIMENTO_PREFIX + id;
    }

    private Long gerarProximoId() {
        return redisTemplate.opsForValue().increment(ID_COUNTER_KEY);
    }

    @Override
    public Atendimento criar(Atendimento atendimento) {
        if (atendimento == null) {
            throw new IllegalArgumentException("Atendimento não pode ser null");
        }

        atendimento.setId(gerarProximoId());
        atendimento.setStatus(StatusAtendimento.AGUARDANDO_ATENDIMENTO);
        atendimento.setDataHoraCriacao(LocalDateTime.now());

        String key = getAtendimentoKey(atendimento.getId());
        redisTemplate.opsForValue().set(key, atendimento);
        redisTemplate.opsForSet().add(ATENDIMENTOS_IDS_KEY, atendimento.getId());

        log.info("Atendimento criado no Redis: ID={}, Cliente={}, Time={}",
                atendimento.getId(), atendimento.getNomeCliente(), atendimento.getTime());

        distribuidorService.distribuir(atendimento);

        return atendimento;
    }

    @Override
    public Optional<Atendimento> buscarPorId(Long id) {
        String key = getAtendimentoKey(id);
        Object obj = redisTemplate.opsForValue().get(key);

        if (obj == null) {
            return Optional.empty();
        }

        // Se já é um Atendimento, retorna diretamente
        if (obj instanceof Atendimento) {
            return Optional.of((Atendimento) obj);
        }

        // Se é um LinkedHashMap (deserialização do Redis), converte manualmente
        if (obj instanceof Map) {
            try {
                Map<String, Object> map = (Map<String, Object>) obj;
                Atendimento atendimento = Atendimento.builder()
                        .id(getLongFromMap(map, "id"))
                        .nomeCliente((String) map.get("nomeCliente"))
                        .assunto((String) map.get("assunto"))
                        .time(Time.valueOf((String) map.get("time")))
                        .status(StatusAtendimento.valueOf((String) map.get("status")))
                        .atendenteId(getLongFromMap(map, "atendenteId"))
                        .dataHoraCriacao(parseLocalDateTime(map.get("dataHoraCriacao")))
                        .dataHoraAtendimento(parseLocalDateTime(map.get("dataHoraAtendimento")))
                        .dataHoraFinalizacao(parseLocalDateTime(map.get("dataHoraFinalizacao")))
                        .build();
                return Optional.of(atendimento);
            } catch (Exception e) {
                log.error("Erro ao converter Map para Atendimento: {}", e.getMessage());
                return Optional.empty();
            }
        }

        log.warn("Objeto do Redis não é Atendimento nem Map: {}", obj.getClass());
        return Optional.empty();
    }

    private Long getLongFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private LocalDateTime parseLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof String) {
            return LocalDateTime.parse((String) value);
        }
        if (value instanceof List) {
            // Redis pode serializar LocalDateTime como array [year, month, day, hour, minute, second, nano]
            List<?> list = (List<?>) value;
            if (list.size() >= 7) {
                return LocalDateTime.of(
                        ((Number) list.get(0)).intValue(),
                        ((Number) list.get(1)).intValue(),
                        ((Number) list.get(2)).intValue(),
                        ((Number) list.get(3)).intValue(),
                        ((Number) list.get(4)).intValue(),
                        ((Number) list.get(5)).intValue(),
                        ((Number) list.get(6)).intValue()
                );
            }
        }
        return null;
    }

    @Override
    public List<Atendimento> listarPorTime(Time time) {
        return listarTodos().stream()
                .filter(a -> a.getTime() == time)
                .collect(Collectors.toList());
    }

    @Override
    public List<Atendimento> listarPorStatus(StatusAtendimento status) {
        return listarTodos().stream()
                .filter(a -> a.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public List<Atendimento> listarTodos() {
        Set<Object> ids = redisTemplate.opsForSet().members(ATENDIMENTOS_IDS_KEY);

        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        List<Atendimento> atendimentos = new ArrayList<>();
        for (Object idObj : ids) {
            Long id = ((Number) idObj).longValue();
            buscarPorId(id).ifPresent(atendimentos::add);
        }

        return atendimentos;
    }
}
