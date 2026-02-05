package com.flowpay.atendimento.dto.response;

import com.flowpay.atendimento.model.Atendimento;
import com.flowpay.atendimento.model.StatusAtendimento;
import com.flowpay.atendimento.model.Time;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de resposta com dados do atendimento.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtendimentoResponse {

    private Long id;
    private String nomeCliente;
    private String assunto;
    private Time time;
    private StatusAtendimento status;
    private Long atendenteId;
    private String nomeAtendente;
    private LocalDateTime dataHoraCriacao;
    private LocalDateTime dataHoraAtendimento;
    private LocalDateTime dataHoraFinalizacao;

    /**
     * Converte uma entidade Atendimento para DTO.
     */
    public static AtendimentoResponse fromEntity(Atendimento atendimento) {
        return AtendimentoResponse.builder()
                .id(atendimento.getId())
                .nomeCliente(atendimento.getNomeCliente())
                .assunto(atendimento.getAssunto())
                .time(atendimento.getTime())
                .status(atendimento.getStatus())
                .atendenteId(atendimento.getAtendenteId())
                .dataHoraCriacao(atendimento.getDataHoraCriacao())
                .dataHoraAtendimento(atendimento.getDataHoraAtendimento())
                .dataHoraFinalizacao(atendimento.getDataHoraFinalizacao())
                .build();
    }
}
