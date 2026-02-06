package com.flowpay.atendimento.dto.websocket;

import com.flowpay.atendimento.model.Time;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NovoAtendenteMessage {

    private Long atendenteId;
    private String nome;
    private Time time;
    private int atendimentosAtivos;
}
