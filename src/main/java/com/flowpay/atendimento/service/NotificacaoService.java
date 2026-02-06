package com.flowpay.atendimento.service;

import com.flowpay.atendimento.model.Atendente;
import com.flowpay.atendimento.model.Atendimento;
import com.flowpay.atendimento.model.Time;


public interface NotificacaoService {

    void notificarNovoAtendimento(Atendimento atendimento);

    void notificarAtualizacaoFila(Time time);

    void notificarAtendimentoFinalizado(Atendimento atendimento);

    void notificarNovoAtendente(Atendente atendente);
}
