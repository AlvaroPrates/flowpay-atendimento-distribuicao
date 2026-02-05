package com.flowpay.atendimento.controller;

import com.flowpay.atendimento.dto.response.AtendenteResponse;
import com.flowpay.atendimento.dto.response.AtendimentoResponse;
import com.flowpay.atendimento.dto.response.DashboardMetricasResponse;
import com.flowpay.atendimento.dto.response.TimeStatusResponse;
import com.flowpay.atendimento.model.StatusAtendimento;
import com.flowpay.atendimento.model.Time;
import com.flowpay.atendimento.service.AtendenteService;
import com.flowpay.atendimento.service.AtendimentoService;
import com.flowpay.atendimento.service.FilaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller REST para métricas e dados do dashboard.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DashboardController {

    private final AtendimentoService atendimentoService;
    private final AtendenteService atendenteService;
    private final FilaService filaService;

    /**
     * GET /api/dashboard/metricas
     * Retorna métricas gerais do sistema.
     */
    @GetMapping("/metricas")
    public ResponseEntity<DashboardMetricasResponse> obterMetricas() {

        // Conta atendimentos ativos
        int totalAtivos = atendimentoService.listarPorStatus(StatusAtendimento.EM_ATENDIMENTO)
                .size();

        // Conta total na fila (soma de todos os times)
        int totalFila = Arrays.stream(Time.values())
                .mapToInt(filaService::tamanhoFila)
                .sum();

        // Conta atendentes
        List<com.flowpay.atendimento.model.Atendente> todosAtendentes = atendenteService.listarTodos();
        int totalAtendentes = todosAtendentes.size();
        int atendentesDisponiveis = (int) todosAtendentes.stream()
                .filter(com.flowpay.atendimento.model.Atendente::isDisponivel)
                .count();

        // Monta maps por time
        Map<Time, Integer> filasPorTime = new HashMap<>();
        Map<Time, Integer> ativosPorTime = new HashMap<>();

        for (Time time : Time.values()) {
            filasPorTime.put(time, filaService.tamanhoFila(time));

            int ativos = (int) atendimentoService.listarPorTime(time)
                    .stream()
                    .filter(a -> a.getStatus() == StatusAtendimento.EM_ATENDIMENTO)
                    .count();
            ativosPorTime.put(time, ativos);
        }

        DashboardMetricasResponse metricas = DashboardMetricasResponse.builder()
                .totalAtendimentosAtivos(totalAtivos)
                .totalNaFila(totalFila)
                .totalAtendentes(totalAtendentes)
                .atendentesDisponiveis(atendentesDisponiveis)
                .filasPorTime(filasPorTime)
                .atendimentosAtivosPorTime(ativosPorTime)
                .build();

        return ResponseEntity.ok(metricas);
    }

    /**
     * GET /api/dashboard/time/{time}
     * Retorna status completo de um time específico.
     */
    @GetMapping("/time/{time}")
    public ResponseEntity<TimeStatusResponse> obterStatusTime(@PathVariable Time time) {

        // Atendentes do time
        List<AtendenteResponse> atendentes = atendenteService.listarPorTime(time)
                .stream()
                .map(AtendenteResponse::fromEntity)
                .collect(Collectors.toList());

        // Fila do time
        List<AtendimentoResponse> fila = filaService.listarFila(time)
                .stream()
                .map(AtendimentoResponse::fromEntity)
                .collect(Collectors.toList());

        // Atendimentos ativos
        int ativos = (int) atendimentoService.listarPorTime(time)
                .stream()
                .filter(a -> a.getStatus() == StatusAtendimento.EM_ATENDIMENTO)
                .count();

        TimeStatusResponse status = TimeStatusResponse.builder()
                .time(time)
                .tamanhoFila(filaService.tamanhoFila(time))
                .atendimentosAtivos(ativos)
                .atendentes(atendentes)
                .fila(fila)
                .build();

        return ResponseEntity.ok(status);
    }
}
