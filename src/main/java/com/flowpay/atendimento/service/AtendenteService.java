package com.flowpay.atendimento.service;

import com.flowpay.atendimento.model.Atendente;
import com.flowpay.atendimento.model.Time;

import java.util.List;
import java.util.Optional;

public interface AtendenteService {

   Atendente cadastrar(Atendente atendente);

   List<Atendente> buscarDisponiveis(Time time);

   Optional<Atendente> buscarPorId(Long id);

   List<Atendente> listarPorTime(Time time);

   List<Atendente> listarTodos();
}
