package com.flowpay.atendimento.service;

import com.flowpay.atendimento.model.Atendimento;
import com.flowpay.atendimento.model.StatusAtendimento;
import com.flowpay.atendimento.model.Time;

import java.util.List;
import java.util.Optional;

public interface AtendimentoService {

   Atendimento criar(Atendimento atendimento);

   Optional<Atendimento> buscarPorId(Long id);

   List<Atendimento> listarPorTime(Time time);

   List<Atendimento> listarPorStatus(StatusAtendimento status);

   List<Atendimento> listarTodos();
}
