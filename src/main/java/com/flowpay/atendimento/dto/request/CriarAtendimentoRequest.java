package com.flowpay.atendimento.dto.request;

import com.flowpay.atendimento.model.Time;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para criação de novo atendimento.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriarAtendimentoRequest {

    @NotBlank(message = "Nome do cliente é obrigatório")
    private String nomeCliente;

    @NotBlank(message = "Assunto é obrigatório")
    private String assunto;

    @NotNull(message = "Time é obrigatório")
    private Time time;
}
