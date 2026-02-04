package com.flowpay.atendimento.service;

import com.flowpay.atendimento.model.Atendimento;
import com.flowpay.atendimento.model.Time;

import java.util.List;

public interface FilaService {

    void enfileirar(Atendimento atendimento);

    Atendimento desenfileirar(Time time);

    List<Atendimento> listarFila(Time time);

    int tamanhoFila(Time time);

    void limparFila(Time time);
}
