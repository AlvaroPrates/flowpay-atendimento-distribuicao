package com.flowpay.atendimento.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Atendimento {
    private Long id;
    private String nomeCliente;
    private String assunto;
    private Time time;
    private StatusAtendimento status;
    private Long atendenteId;
    private LocalDateTime dataHoraCriacao;
    private LocalDateTime dataHoraAtendimento;
    private LocalDateTime dataHoraFinalizacao;
}
