package com.flowpay.atendimento.dto.response;

import com.flowpay.atendimento.model.Time;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO com métricas gerais do dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricasResponse {

    private int totalAtendimentosAtivos;
    private int totalNaFila;
    private int totalAtendentes;
    private int atendentesDisponiveis;
    private Map<Time, Integer> filasPorTime;
    private Map<Time, Integer> atendimentosAtivosPorTime;
}
