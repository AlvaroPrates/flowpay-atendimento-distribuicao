package com.flowpay.atendimento.service.impl.redis;

import com.flowpay.atendimento.model.Atendimento;
import com.flowpay.atendimento.model.StatusAtendimento;
import com.flowpay.atendimento.model.Time;
import com.flowpay.atendimento.service.FilaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Profile("redis")
@RequiredArgsConstructor
@Slf4j
public class RedisFilaService implements FilaService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String FILA_PREFIX = "fila:";

    private String getFilaKey(Time time) {
        return FILA_PREFIX + time.name();
    }

    @Override
    public void enfileirar(Atendimento atendimento) {
        if (atendimento == null) {
            log.warn("Tentativa de enfileirar atendimento null");
            return;
        }

        String key = getFilaKey(atendimento.getTime());
        log.info("Enfileirando no Redis: key={}, atendimentoId={}", key, atendimento.getId());

        redisTemplate.opsForList().rightPush(key, atendimento);

        log.debug("Fila Redis '{}' agora tem {} itens", key, redisTemplate.opsForList().size(key));
    }

    @Override
    public Atendimento desenfileirar(Time time) {
        String key = getFilaKey(time);
        Object obj = redisTemplate.opsForList().leftPop(key);

        if (obj != null) {
            Atendimento atendimento = convertToAtendimento(obj);
            if (atendimento != null) {
                log.info("Desenfileirado do Redis: key={}, atendimentoId={}", key, atendimento.getId());
                return atendimento;
            } else {
                log.error("Erro ao converter objeto da fila Redis para Atendimento");
                return null;
            }
        }

        log.debug("Fila Redis '{}' está vazia", key);
        return null;
    }

    @Override
    public List<Atendimento> listarFila(Time time) {
        String key = getFilaKey(time);
        List<Object> objects = redisTemplate.opsForList().range(key, 0, -1);

        if (objects == null || objects.isEmpty()) {
            return new ArrayList<>();
        }

        List<Atendimento> atendimentos = new ArrayList<>();
        for (Object obj : objects) {
            Atendimento atendimento = convertToAtendimento(obj);
            if (atendimento != null) {
                atendimentos.add(atendimento);
            } else {
                log.warn("Objeto inválido na fila do Redis foi ignorado");
            }
        }

        return atendimentos;
    }

    @Override
    public int tamanhoFila(Time time) {
        String key = getFilaKey(time);
        Long size = redisTemplate.opsForList().size(key);
        return size != null ? size.intValue() : 0;
    }

    @Override
    public void limparFila(Time time) {
        String key = getFilaKey(time);
        Long tamanho = redisTemplate.opsForList().size(key);
        redisTemplate.delete(key);
        log.info("Fila Redis '{}' limpa. Removidos {} atendimentos", key, tamanho);
    }

    /**
     * Converte objeto do Redis para Atendimento.
     * Lida com deserialização tanto de objetos Atendimento diretos quanto LinkedHashMap.
     */
    private Atendimento convertToAtendimento(Object obj) {
        if (obj == null) {
            return null;
        }

        // Se já é um Atendimento, retorna diretamente
        if (obj instanceof Atendimento) {
            return (Atendimento) obj;
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
                return atendimento;
            } catch (Exception e) {
                log.error("Erro ao converter Map para Atendimento: {}", e.getMessage());
                return null;
            }
        }

        log.warn("Objeto do Redis não é Atendimento nem Map: {}", obj.getClass());
        return null;
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
}
