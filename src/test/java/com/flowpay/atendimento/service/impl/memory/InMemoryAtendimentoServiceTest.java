package com.flowpay.atendimento.service.impl.memory;

import com.flowpay.atendimento.model.Atendimento;
import com.flowpay.atendimento.model.StatusAtendimento;
import com.flowpay.atendimento.model.Time;
import com.flowpay.atendimento.service.DistribuidorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InMemoryAtendimentoServiceTest {

    @Mock
    private DistribuidorService distribuidorService;

    private InMemoryAtendimentoService service;

    @BeforeEach
    void setUp() {
        service = new InMemoryAtendimentoService(distribuidorService);
    }

    @Test
    void criar_DeveGerarIdAutomaticamente() {
        Atendimento atendimento = Atendimento.builder()
                .nomeCliente("João Silva")
                .assunto("Problema com cartão")
                .time(Time.CARTOES)
                .build();

        Atendimento resultado = service.criar(atendimento);

        assertNotNull(resultado.getId());
        assertEquals(1L, resultado.getId());
    }

    @Test
    void criar_DeveDefinirStatusComoAguardandoAtendimento() {
        Atendimento atendimento = Atendimento.builder()
                .nomeCliente("Maria Santos")
                .assunto("Solicitação de empréstimo")
                .time(Time.EMPRESTIMOS)
                .build();

        Atendimento resultado = service.criar(atendimento);

        assertEquals(StatusAtendimento.AGUARDANDO_ATENDIMENTO, resultado.getStatus());
    }

    @Test
    void criar_DeveDefinirDataHoraCriacao() {
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

        Atendimento atendimento = Atendimento.builder()
                .nomeCliente("Pedro Costa")
                .assunto("Dúvida sobre conta")
                .time(Time.OUTROS)
                .build();

        Atendimento resultado = service.criar(atendimento);

        LocalDateTime depois = LocalDateTime.now().plusSeconds(1);

        assertNotNull(resultado.getDataHoraCriacao());
        assertTrue(resultado.getDataHoraCriacao().isAfter(antes));
        assertTrue(resultado.getDataHoraCriacao().isBefore(depois));
    }

    @Test
    void criar_DeveLancarExcecao_QuandoAtendimentoNulo() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.criar(null)
        );

        assertEquals("Atendimento não pode ser null", exception.getMessage());
    }

    @Test
    void criar_DeveChamarDistribuidor() {
        Atendimento atendimento = Atendimento.builder()
                .nomeCliente("Ana Paula")
                .assunto("Bloqueio de cartão")
                .time(Time.CARTOES)
                .build();

        service.criar(atendimento);

        verify(distribuidorService, times(1)).distribuir(any(Atendimento.class));
    }

    @Test
    void criar_DeveIncrementarIdSequencialmente() {
        Atendimento atendimento1 = Atendimento.builder()
                .nomeCliente("Cliente 1")
                .assunto("Assunto 1")
                .time(Time.CARTOES)
                .build();

        Atendimento atendimento2 = Atendimento.builder()
                .nomeCliente("Cliente 2")
                .assunto("Assunto 2")
                .time(Time.EMPRESTIMOS)
                .build();

        Atendimento resultado1 = service.criar(atendimento1);
        Atendimento resultado2 = service.criar(atendimento2);

        assertEquals(1L, resultado1.getId());
        assertEquals(2L, resultado2.getId());
    }

    @Test
    void criar_DevePreservarDadosDoAtendimento() {
        Atendimento atendimento = Atendimento.builder()
                .nomeCliente("Carlos Eduardo")
                .assunto("Reclamação sobre taxa")
                .time(Time.OUTROS)
                .build();

        Atendimento resultado = service.criar(atendimento);

        assertEquals("Carlos Eduardo", resultado.getNomeCliente());
        assertEquals("Reclamação sobre taxa", resultado.getAssunto());
        assertEquals(Time.OUTROS, resultado.getTime());
    }

    @Test
    void buscarPorId_DeveRetornarEmpty_QuandoIdNaoExiste() {
        Optional<Atendimento> resultado = service.buscarPorId(999L);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void buscarPorId_DeveRetornarAtendimento_QuandoIdExiste() {
        Atendimento atendimento = service.criar(Atendimento.builder()
                .nomeCliente("João Silva")
                .assunto("Problema com cartão")
                .time(Time.CARTOES)
                .build());

        Optional<Atendimento> resultado = service.buscarPorId(atendimento.getId());

        assertTrue(resultado.isPresent());
        assertEquals("João Silva", resultado.get().getNomeCliente());
        assertEquals("Problema com cartão", resultado.get().getAssunto());
        assertEquals(Time.CARTOES, resultado.get().getTime());
    }

    @Test
    void listarPorTime_DeveRetornarListaVazia_QuandoNaoHaAtendimentosDoTime() {
        service.criar(Atendimento.builder()
                .nomeCliente("João")
                .assunto("Assunto")
                .time(Time.CARTOES)
                .build());

        List<Atendimento> resultado = service.listarPorTime(Time.EMPRESTIMOS);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void listarPorTime_DeveRetornarApenasMesmoTime() {
        service.criar(Atendimento.builder()
                .nomeCliente("João")
                .assunto("Problema com cartão")
                .time(Time.CARTOES)
                .build());

        service.criar(Atendimento.builder()
                .nomeCliente("Maria")
                .assunto("Solicitar empréstimo")
                .time(Time.EMPRESTIMOS)
                .build());

        service.criar(Atendimento.builder()
                .nomeCliente("Pedro")
                .assunto("Outro problema cartão")
                .time(Time.CARTOES)
                .build());

        List<Atendimento> resultado = service.listarPorTime(Time.CARTOES);

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(a -> a.getTime() == Time.CARTOES));
    }

    @Test
    void listarPorTime_DeveIncluirTodosStatus() {
        Atendimento atendimento1 = service.criar(Atendimento.builder()
                .nomeCliente("João")
                .assunto("Assunto 1")
                .time(Time.CARTOES)
                .build());

        Atendimento atendimento2 = service.criar(Atendimento.builder()
                .nomeCliente("Maria")
                .assunto("Assunto 2")
                .time(Time.CARTOES)
                .build());

        atendimento2.setStatus(StatusAtendimento.EM_ATENDIMENTO);

        List<Atendimento> resultado = service.listarPorTime(Time.CARTOES);

        assertEquals(2, resultado.size());
    }

    @Test
    void listarPorStatus_DeveRetornarListaVazia_QuandoNaoHaAtendimentosComStatus() {
        service.criar(Atendimento.builder()
                .nomeCliente("João")
                .assunto("Assunto")
                .time(Time.CARTOES)
                .build());

        List<Atendimento> resultado = service.listarPorStatus(StatusAtendimento.FINALIZADO);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void listarPorStatus_DeveRetornarApenasAtendimentosComStatus() {
        service.criar(Atendimento.builder()
                .nomeCliente("João")
                .assunto("Assunto 1")
                .time(Time.CARTOES)
                .build());

        Atendimento atendimento2 = service.criar(Atendimento.builder()
                .nomeCliente("Maria")
                .assunto("Assunto 2")
                .time(Time.EMPRESTIMOS)
                .build());

        Atendimento atendimento3 = service.criar(Atendimento.builder()
                .nomeCliente("Pedro")
                .assunto("Assunto 3")
                .time(Time.OUTROS)
                .build());

        atendimento2.setStatus(StatusAtendimento.EM_ATENDIMENTO);
        atendimento3.setStatus(StatusAtendimento.EM_ATENDIMENTO);

        List<Atendimento> resultado = service.listarPorStatus(StatusAtendimento.EM_ATENDIMENTO);

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(a -> a.getStatus() == StatusAtendimento.EM_ATENDIMENTO));
    }

    @Test
    void listarPorStatus_DeveIncluirTodosTimes() {
        service.criar(Atendimento.builder()
                .nomeCliente("João")
                .assunto("Assunto 1")
                .time(Time.CARTOES)
                .build());

        service.criar(Atendimento.builder()
                .nomeCliente("Maria")
                .assunto("Assunto 2")
                .time(Time.EMPRESTIMOS)
                .build());

        List<Atendimento> resultado = service.listarPorStatus(StatusAtendimento.AGUARDANDO_ATENDIMENTO);

        assertEquals(2, resultado.size());
    }

    @Test
    void listarTodos_DeveRetornarListaVazia_QuandoNaoHaAtendimentos() {
        List<Atendimento> resultado = service.listarTodos();

        assertTrue(resultado.isEmpty());
    }

    @Test
    void listarTodos_DeveRetornarTodosAtendimentos() {
        service.criar(Atendimento.builder()
                .nomeCliente("João")
                .assunto("Assunto 1")
                .time(Time.CARTOES)
                .build());

        service.criar(Atendimento.builder()
                .nomeCliente("Maria")
                .assunto("Assunto 2")
                .time(Time.EMPRESTIMOS)
                .build());

        service.criar(Atendimento.builder()
                .nomeCliente("Pedro")
                .assunto("Assunto 3")
                .time(Time.OUTROS)
                .build());

        List<Atendimento> resultado = service.listarTodos();

        assertEquals(3, resultado.size());
    }

    @Test
    void listarTodos_DeveRetornarTodosTimesEStatus() {
        Atendimento atendimento1 = service.criar(Atendimento.builder()
                .nomeCliente("João")
                .assunto("Assunto 1")
                .time(Time.CARTOES)
                .build());

        Atendimento atendimento2 = service.criar(Atendimento.builder()
                .nomeCliente("Maria")
                .assunto("Assunto 2")
                .time(Time.EMPRESTIMOS)
                .build());

        atendimento2.setStatus(StatusAtendimento.FINALIZADO);

        List<Atendimento> resultado = service.listarTodos();

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().anyMatch(a -> a.getStatus() == StatusAtendimento.AGUARDANDO_ATENDIMENTO));
        assertTrue(resultado.stream().anyMatch(a -> a.getStatus() == StatusAtendimento.FINALIZADO));
    }

    @Test
    void listarTodos_DeveRetornarCopiaImutavel() {
        service.criar(Atendimento.builder()
                .nomeCliente("João")
                .assunto("Assunto")
                .time(Time.CARTOES)
                .build());

        List<Atendimento> resultado = service.listarTodos();
        int tamanhoInicial = resultado.size();

        service.criar(Atendimento.builder()
                .nomeCliente("Maria")
                .assunto("Outro assunto")
                .time(Time.EMPRESTIMOS)
                .build());

        assertEquals(tamanhoInicial, resultado.size());
    }
}