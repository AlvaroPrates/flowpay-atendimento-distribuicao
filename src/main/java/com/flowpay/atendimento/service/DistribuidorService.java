package com.flowpay.atendimento.service;

import com.flowpay.atendimento.model.Atendimento;
import com.flowpay.atendimento.model.Time;

public interface DistribuidorService {

   void distribuir(Atendimento atendimento);

   void finalizarAtendimento(Long atendimentoId);

   void processarFila(Time time);
}
