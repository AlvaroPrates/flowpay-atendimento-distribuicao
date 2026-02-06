package com.flowpay.atendimento.service.impl.memory;

import com.flowpay.atendimento.model.Atendente;
import com.flowpay.atendimento.model.Time;
import com.flowpay.atendimento.service.DistribuidorService;
import com.flowpay.atendimento.service.NotificacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InMemoryAtendenteServiceTest {

    @Mock
    private NotificacaoService notificacaoService;

    @Mock
    private DistribuidorService distribuidorService;

    private InMemoryAtendenteService service;

    @BeforeEach
    void setUp() {
        service = new InMemoryAtendenteService(notificacaoService, distribuidorService);
    }

    @Test
    void cadastrar_DeveGerarIdAutomaticamente_QuandoIdNulo() {
        Atendente atendente = Atendente.builder()
                .nome("João")
                .time(Time.CARTOES)
                .build();

        Atendente resultado = service.cadastrar(atendente);

        assertNotNull(resultado.getId());
        assertEquals(1L, resultado.getId());
    }

    @Test
    void cadastrar_DeveGerarIdAutomaticamente_QuandoIdZero() {
        Atendente atendente = Atendente.builder()
                .id(0L)
                .nome("Maria")
                .time(Time.EMPRESTIMOS)
                .build();

        Atendente resultado = service.cadastrar(atendente);

        assertNotNull(resultado.getId());
        assertEquals(1L, resultado.getId());
    }

    @Test
    void cadastrar_DeveManterIdInformado_QuandoIdValido() {
        Atendente atendente = Atendente.builder()
                .id(100L)
                .nome("Pedro")
                .time(Time.OUTROS)
                .build();

        Atendente resultado = service.cadastrar(atendente);

        assertEquals(100L, resultado.getId());
    }

    @Test
    void cadastrar_DeveResetarAtendimentosAtivos() {
        Atendente atendente = Atendente.builder()
                .nome("Ana")
                .time(Time.CARTOES)
                .atendimentosAtivos(5)
                .build();

        Atendente resultado = service.cadastrar(atendente);

        assertEquals(0, resultado.getAtendimentosAtivos());
    }

    @Test
    void cadastrar_DeveLancarExcecao_QuandoAtendenteNulo() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.cadastrar(null)
        );

        assertEquals("Atendente não pode ser null", exception.getMessage());
    }

    @Test
    void cadastrar_DeveNotificarNovoAtendente() {
        Atendente atendente = Atendente.builder()
                .nome("Carlos")
                .time(Time.EMPRESTIMOS)
                .build();

        service.cadastrar(atendente);

        verify(notificacaoService, times(1)).notificarNovoAtendente(any(Atendente.class));
    }

    @Test
    void cadastrar_DeveProcessarFilaDoTime() {
        Atendente atendente = Atendente.builder()
                .nome("Fernanda")
                .time(Time.OUTROS)
                .build();

        service.cadastrar(atendente);

        verify(distribuidorService, times(1)).processarFila(Time.OUTROS);
    }

    @Test
    void cadastrar_DeveIncrementarIdSequencialmente() {
        Atendente atendente1 = Atendente.builder()
                .nome("Atendente 1")
                .time(Time.CARTOES)
                .build();

        Atendente atendente2 = Atendente.builder()
                .nome("Atendente 2")
                .time(Time.EMPRESTIMOS)
                .build();

        Atendente resultado1 = service.cadastrar(atendente1);
        Atendente resultado2 = service.cadastrar(atendente2);

        assertEquals(1L, resultado1.getId());
        assertEquals(2L, resultado2.getId());
    }

    @Test
    void buscarDisponiveisPorTime_DeveRetornarListaVazia_QuandoNaoHaAtendentesDoTime() {
        List<Atendente> resultado = service.buscarDisponiveisPorTime(Time.CARTOES);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void buscarDisponiveisPorTime_DeveRetornarApenasMesmoTime() {
        service.cadastrar(Atendente.builder()
                .nome("João")
                .time(Time.CARTOES)
                .build());

        service.cadastrar(Atendente.builder()
                .nome("Maria")
                .time(Time.EMPRESTIMOS)
                .build());

        List<Atendente> resultado = service.buscarDisponiveisPorTime(Time.CARTOES);

        assertEquals(1, resultado.size());
        assertEquals("João", resultado.get(0).getNome());
        assertEquals(Time.CARTOES, resultado.get(0).getTime());
    }

    @Test
    void buscarDisponiveisPorTime_DeveRetornarApenasDisponiveis() {
        Atendente atendenteDisponivel = Atendente.builder()
                .nome("João")
                .time(Time.CARTOES)
                .build();

        Atendente atendenteIndisponivel = Atendente.builder()
                .nome("Maria")
                .time(Time.CARTOES)
                .build();

        service.cadastrar(atendenteDisponivel);
        Atendente cadastrado = service.cadastrar(atendenteIndisponivel);

        cadastrado.setAtendimentosAtivos(Atendente.MAX_ATENDIMENTOS);

        List<Atendente> resultado = service.buscarDisponiveisPorTime(Time.CARTOES);

        assertEquals(1, resultado.size());
        assertEquals("João", resultado.get(0).getNome());
    }

    @Test
    void buscarDisponiveisPorTime_DeveOrdenarPorAtendimentosAtivos() {
        Atendente atendente1 = service.cadastrar(Atendente.builder()
                .nome("Atendente 1")
                .time(Time.CARTOES)
                .build());

        Atendente atendente2 = service.cadastrar(Atendente.builder()
                .nome("Atendente 2")
                .time(Time.CARTOES)
                .build());

        Atendente atendente3 = service.cadastrar(Atendente.builder()
                .nome("Atendente 3")
                .time(Time.CARTOES)
                .build());

        atendente1.setAtendimentosAtivos(2);
        atendente2.setAtendimentosAtivos(0);
        atendente3.setAtendimentosAtivos(1);

        List<Atendente> resultado = service.buscarDisponiveisPorTime(Time.CARTOES);

        assertEquals(3, resultado.size());
        assertEquals("Atendente 2", resultado.get(0).getNome());
        assertEquals(0, resultado.get(0).getAtendimentosAtivos());
        assertEquals("Atendente 3", resultado.get(1).getNome());
        assertEquals(1, resultado.get(1).getAtendimentosAtivos());
        assertEquals("Atendente 1", resultado.get(2).getNome());
        assertEquals(2, resultado.get(2).getAtendimentosAtivos());
    }

    @Test
    void buscarPorId_DeveRetornarEmpty_QuandoIdNaoExiste() {
        Optional<Atendente> resultado = service.buscarPorId(999L);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void buscarPorId_DeveRetornarAtendente_QuandoIdExiste() {
        Atendente atendente = service.cadastrar(Atendente.builder()
                .nome("João")
                .time(Time.CARTOES)
                .build());

        Optional<Atendente> resultado = service.buscarPorId(atendente.getId());

        assertTrue(resultado.isPresent());
        assertEquals("João", resultado.get().getNome());
        assertEquals(Time.CARTOES, resultado.get().getTime());
    }

    @Test
    void listarPorTime_DeveRetornarListaVazia_QuandoNaoHaAtendentesDoTime() {
        service.cadastrar(Atendente.builder()
                .nome("João")
                .time(Time.CARTOES)
                .build());

        List<Atendente> resultado = service.listarPorTime(Time.EMPRESTIMOS);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void listarPorTime_DeveRetornarTodosAtendentesDoTime() {
        service.cadastrar(Atendente.builder()
                .nome("João")
                .time(Time.CARTOES)
                .build());

        service.cadastrar(Atendente.builder()
                .nome("Maria")
                .time(Time.CARTOES)
                .build());

        service.cadastrar(Atendente.builder()
                .nome("Pedro")
                .time(Time.EMPRESTIMOS)
                .build());

        List<Atendente> resultado = service.listarPorTime(Time.CARTOES);

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(a -> a.getTime() == Time.CARTOES));
    }

    @Test
    void listarPorTime_DeveIncluirAtendentesIndisponiveis() {
        Atendente atendente = service.cadastrar(Atendente.builder()
                .nome("João")
                .time(Time.CARTOES)
                .build());

        atendente.setAtendimentosAtivos(Atendente.MAX_ATENDIMENTOS);

        List<Atendente> resultado = service.listarPorTime(Time.CARTOES);

        assertEquals(1, resultado.size());
        assertFalse(resultado.get(0).isDisponivel());
    }

    @Test
    void listarTodos_DeveRetornarListaVazia_QuandoNaoHaAtendentes() {
        List<Atendente> resultado = service.listarTodos();

        assertTrue(resultado.isEmpty());
    }

    @Test
    void listarTodos_DeveRetornarTodosAtendentes() {
        service.cadastrar(Atendente.builder()
                .nome("João")
                .time(Time.CARTOES)
                .build());

        service.cadastrar(Atendente.builder()
                .nome("Maria")
                .time(Time.EMPRESTIMOS)
                .build());

        service.cadastrar(Atendente.builder()
                .nome("Pedro")
                .time(Time.OUTROS)
                .build());

        List<Atendente> resultado = service.listarTodos();

        assertEquals(3, resultado.size());
    }

    @Test
    void listarTodos_DeveRetornarCopiaImutavel() {
        service.cadastrar(Atendente.builder()
                .nome("João")
                .time(Time.CARTOES)
                .build());

        List<Atendente> resultado = service.listarTodos();
        int tamanhoInicial = resultado.size();

        service.cadastrar(Atendente.builder()
                .nome("Maria")
                .time(Time.EMPRESTIMOS)
                .build());

        assertEquals(tamanhoInicial, resultado.size());
    }
}