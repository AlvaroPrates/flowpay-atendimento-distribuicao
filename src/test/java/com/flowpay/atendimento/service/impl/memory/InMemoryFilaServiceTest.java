package com.flowpay.atendimento.service.impl.memory;

import com.flowpay.atendimento.model.Atendimento;
import com.flowpay.atendimento.model.StatusAtendimento;
import com.flowpay.atendimento.model.Time;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryFilaServiceTest {

    private InMemoryFilaService service;

    @BeforeEach
    void setUp() {
        service = new InMemoryFilaService();
    }

    @Test
    void construtor_DeveInicializarFilasParaTodosTimes() {
        for (Time time : Time.values()) {
            assertEquals(0, service.tamanhoFila(time));
        }
    }

    @Test
    void enfileirar_DeveAdicionarAtendimentoNaFila() {
        Atendimento atendimento = Atendimento.builder()
                .id(1L)
                .nomeCliente("João Silva")
                .assunto("Problema com cartão")
                .time(Time.CARTOES)
                .status(StatusAtendimento.AGUARDANDO_ATENDIMENTO)
                .build();

        service.enfileirar(atendimento);

        assertEquals(1, service.tamanhoFila(Time.CARTOES));
    }

    @Test
    void enfileirar_DeveIgnorarAtendimentoNull() {
        service.enfileirar(null);

        assertEquals(0, service.tamanhoFila(Time.CARTOES));
        assertEquals(0, service.tamanhoFila(Time.EMPRESTIMOS));
        assertEquals(0, service.tamanhoFila(Time.OUTROS));
    }

    @Test
    void enfileirar_DeveAdicionarMultiplosAtendimentosNaMesmaFila() {
        Atendimento atendimento1 = Atendimento.builder()
                .id(1L)
                .nomeCliente("João")
                .assunto("Assunto 1")
                .time(Time.CARTOES)
                .build();

        Atendimento atendimento2 = Atendimento.builder()
                .id(2L)
                .nomeCliente("Maria")
                .assunto("Assunto 2")
                .time(Time.CARTOES)
                .build();

        service.enfileirar(atendimento1);
        service.enfileirar(atendimento2);

        assertEquals(2, service.tamanhoFila(Time.CARTOES));
    }

    @Test
    void enfileirar_DeveManterFilasSeparadasPorTime() {
        Atendimento atendimentoCartoes = Atendimento.builder()
                .id(1L)
                .nomeCliente("João")
                .assunto("Assunto Cartões")
                .time(Time.CARTOES)
                .build();

        Atendimento atendimentoEmprestimos = Atendimento.builder()
                .id(2L)
                .nomeCliente("Maria")
                .assunto("Assunto Empréstimos")
                .time(Time.EMPRESTIMOS)
                .build();

        service.enfileirar(atendimentoCartoes);
        service.enfileirar(atendimentoEmprestimos);

        assertEquals(1, service.tamanhoFila(Time.CARTOES));
        assertEquals(1, service.tamanhoFila(Time.EMPRESTIMOS));
        assertEquals(0, service.tamanhoFila(Time.OUTROS));
    }

    @Test
    void desenfileirar_DeveRetornarNull_QuandoFilaVazia() {
        Atendimento resultado = service.desenfileirar(Time.CARTOES);

        assertNull(resultado);
    }

    @Test
    void desenfileirar_DeveRemoverERetornarPrimeiroAtendimento() {
        Atendimento atendimento = Atendimento.builder()
                .id(1L)
                .nomeCliente("João Silva")
                .assunto("Problema com cartão")
                .time(Time.CARTOES)
                .build();

        service.enfileirar(atendimento);

        Atendimento resultado = service.desenfileirar(Time.CARTOES);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("João Silva", resultado.getNomeCliente());
        assertEquals(0, service.tamanhoFila(Time.CARTOES));
    }

    @Test
    void desenfileirar_DeveRespeitarOrdemFIFO() {
        Atendimento atendimento1 = Atendimento.builder()
                .id(1L)
                .nomeCliente("Primeiro")
                .assunto("Assunto 1")
                .time(Time.CARTOES)
                .build();

        Atendimento atendimento2 = Atendimento.builder()
                .id(2L)
                .nomeCliente("Segundo")
                .assunto("Assunto 2")
                .time(Time.CARTOES)
                .build();

        Atendimento atendimento3 = Atendimento.builder()
                .id(3L)
                .nomeCliente("Terceiro")
                .assunto("Assunto 3")
                .time(Time.CARTOES)
                .build();

        service.enfileirar(atendimento1);
        service.enfileirar(atendimento2);
        service.enfileirar(atendimento3);

        assertEquals(1L, service.desenfileirar(Time.CARTOES).getId());
        assertEquals(2L, service.desenfileirar(Time.CARTOES).getId());
        assertEquals(3L, service.desenfileirar(Time.CARTOES).getId());
        assertNull(service.desenfileirar(Time.CARTOES));
    }

    @Test
    void desenfileirar_DeveApenasAfetarFilaDoTimeEspecifico() {
        Atendimento atendimentoCartoes = Atendimento.builder()
                .id(1L)
                .nomeCliente("João")
                .assunto("Cartões")
                .time(Time.CARTOES)
                .build();

        Atendimento atendimentoEmprestimos = Atendimento.builder()
                .id(2L)
                .nomeCliente("Maria")
                .assunto("Empréstimos")
                .time(Time.EMPRESTIMOS)
                .build();

        service.enfileirar(atendimentoCartoes);
        service.enfileirar(atendimentoEmprestimos);

        service.desenfileirar(Time.CARTOES);

        assertEquals(0, service.tamanhoFila(Time.CARTOES));
        assertEquals(1, service.tamanhoFila(Time.EMPRESTIMOS));
    }

    @Test
    void listarFila_DeveRetornarListaVazia_QuandoFilaVazia() {
        List<Atendimento> resultado = service.listarFila(Time.CARTOES);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void listarFila_DeveRetornarTodosAtendimentosDaFila() {
        Atendimento atendimento1 = Atendimento.builder()
                .id(1L)
                .nomeCliente("João")
                .assunto("Assunto 1")
                .time(Time.CARTOES)
                .build();

        Atendimento atendimento2 = Atendimento.builder()
                .id(2L)
                .nomeCliente("Maria")
                .assunto("Assunto 2")
                .time(Time.CARTOES)
                .build();

        service.enfileirar(atendimento1);
        service.enfileirar(atendimento2);

        List<Atendimento> resultado = service.listarFila(Time.CARTOES);

        assertEquals(2, resultado.size());
        assertEquals(1L, resultado.get(0).getId());
        assertEquals(2L, resultado.get(1).getId());
    }

    @Test
    void listarFila_DeveRetornarApenasFilaDoTimeEspecifico() {
        Atendimento atendimentoCartoes = Atendimento.builder()
                .id(1L)
                .nomeCliente("João")
                .assunto("Cartões")
                .time(Time.CARTOES)
                .build();

        Atendimento atendimentoEmprestimos = Atendimento.builder()
                .id(2L)
                .nomeCliente("Maria")
                .assunto("Empréstimos")
                .time(Time.EMPRESTIMOS)
                .build();

        service.enfileirar(atendimentoCartoes);
        service.enfileirar(atendimentoEmprestimos);

        List<Atendimento> resultado = service.listarFila(Time.CARTOES);

        assertEquals(1, resultado.size());
        assertEquals(Time.CARTOES, resultado.get(0).getTime());
    }

    @Test
    void listarFila_DeveRetornarCopiaImutavel() {
        Atendimento atendimento = Atendimento.builder()
                .id(1L)
                .nomeCliente("João")
                .assunto("Assunto")
                .time(Time.CARTOES)
                .build();

        service.enfileirar(atendimento);

        List<Atendimento> resultado = service.listarFila(Time.CARTOES);
        int tamanhoInicial = resultado.size();

        service.enfileirar(Atendimento.builder()
                .id(2L)
                .nomeCliente("Maria")
                .assunto("Outro")
                .time(Time.CARTOES)
                .build());

        assertEquals(tamanhoInicial, resultado.size());
    }

    @Test
    void tamanhoFila_DeveRetornarZero_QuandoFilaVazia() {
        assertEquals(0, service.tamanhoFila(Time.CARTOES));
    }

    @Test
    void tamanhoFila_DeveRetornarQuantidadeCorreta() {
        service.enfileirar(Atendimento.builder()
                .id(1L)
                .nomeCliente("João")
                .time(Time.CARTOES)
                .build());

        service.enfileirar(Atendimento.builder()
                .id(2L)
                .nomeCliente("Maria")
                .time(Time.CARTOES)
                .build());

        service.enfileirar(Atendimento.builder()
                .id(3L)
                .nomeCliente("Pedro")
                .time(Time.CARTOES)
                .build());

        assertEquals(3, service.tamanhoFila(Time.CARTOES));
    }

    @Test
    void tamanhoFila_DeveAtualizarAposDesenfileirar() {
        service.enfileirar(Atendimento.builder()
                .id(1L)
                .nomeCliente("João")
                .time(Time.CARTOES)
                .build());

        service.enfileirar(Atendimento.builder()
                .id(2L)
                .nomeCliente("Maria")
                .time(Time.CARTOES)
                .build());

        assertEquals(2, service.tamanhoFila(Time.CARTOES));

        service.desenfileirar(Time.CARTOES);

        assertEquals(1, service.tamanhoFila(Time.CARTOES));
    }

    @Test
    void limparFila_DeveLimparApenasFilaDoTimeEspecifico() {
        service.enfileirar(Atendimento.builder()
                .id(1L)
                .nomeCliente("João")
                .time(Time.CARTOES)
                .build());

        service.enfileirar(Atendimento.builder()
                .id(2L)
                .nomeCliente("Maria")
                .time(Time.EMPRESTIMOS)
                .build());

        service.limparFila(Time.CARTOES);

        assertEquals(0, service.tamanhoFila(Time.CARTOES));
        assertEquals(1, service.tamanhoFila(Time.EMPRESTIMOS));
    }

    @Test
    void limparFila_DeveRemoverTodosAtendimentos() {
        service.enfileirar(Atendimento.builder()
                .id(1L)
                .nomeCliente("João")
                .time(Time.CARTOES)
                .build());

        service.enfileirar(Atendimento.builder()
                .id(2L)
                .nomeCliente("Maria")
                .time(Time.CARTOES)
                .build());

        service.enfileirar(Atendimento.builder()
                .id(3L)
                .nomeCliente("Pedro")
                .time(Time.CARTOES)
                .build());

        service.limparFila(Time.CARTOES);

        assertEquals(0, service.tamanhoFila(Time.CARTOES));
        assertTrue(service.listarFila(Time.CARTOES).isEmpty());
    }

    @Test
    void limparFila_DeveFuncionarComFilaVazia() {
        service.limparFila(Time.CARTOES);

        assertEquals(0, service.tamanhoFila(Time.CARTOES));
    }

    @Test
    void integracaoCompleta_DeveFuncionarComoEsperado() {
        Atendimento atendimento1 = Atendimento.builder()
                .id(1L)
                .nomeCliente("João")
                .assunto("Assunto 1")
                .time(Time.CARTOES)
                .build();

        Atendimento atendimento2 = Atendimento.builder()
                .id(2L)
                .nomeCliente("Maria")
                .assunto("Assunto 2")
                .time(Time.CARTOES)
                .build();

        service.enfileirar(atendimento1);
        service.enfileirar(atendimento2);
        assertEquals(2, service.tamanhoFila(Time.CARTOES));

        List<Atendimento> fila = service.listarFila(Time.CARTOES);
        assertEquals(2, fila.size());

        Atendimento desenfileirado = service.desenfileirar(Time.CARTOES);
        assertEquals(1L, desenfileirado.getId());
        assertEquals(1, service.tamanhoFila(Time.CARTOES));

        service.limparFila(Time.CARTOES);
        assertEquals(0, service.tamanhoFila(Time.CARTOES));
    }
}
