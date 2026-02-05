package com.flowpay.atendimento.dto.response;

import com.flowpay.atendimento.model.Atendente;
import com.flowpay.atendimento.model.Time;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de resposta com dados do atendente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtendenteResponse {

    private Long id;
    private String nome;
    private Time time;
    private int atendimentosAtivos;
    private int capacidadeMaxima;
    private boolean disponivel;

    /**
     * Converte uma entidade Atendente para DTO.
     */
    public static AtendenteResponse fromEntity(Atendente atendente) {
        return AtendenteResponse.builder()
                .id(atendente.getId())
                .nome(atendente.getNome())
                .time(atendente.getTime())
                .atendimentosAtivos(atendente.getAtendimentosAtivos())
                .capacidadeMaxima(3)
                .disponivel(atendente.isDisponivel())
                .build();
    }
}
