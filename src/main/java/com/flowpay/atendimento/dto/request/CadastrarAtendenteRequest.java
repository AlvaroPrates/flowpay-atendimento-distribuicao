package com.flowpay.atendimento.dto.request;

import com.flowpay.atendimento.model.Time;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para cadastro de novo atendente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CadastrarAtendenteRequest {

    @NotBlank(message = "Nome do atendente é obrigatório")
    private String nome;

    @NotNull(message = "Time é obrigatório")
    private Time time;
}
