package com.flowpay.atendimento.controller;

import com.flowpay.atendimento.dto.request.CadastrarAtendenteRequest;
import com.flowpay.atendimento.dto.response.AtendenteResponse;
import com.flowpay.atendimento.exception.RecursoNaoEncontradoException;
import com.flowpay.atendimento.model.Atendente;
import com.flowpay.atendimento.model.Time;
import com.flowpay.atendimento.service.AtendenteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller REST para gerenciamento de atendentes.
 */
@RestController
@RequestMapping("/api/atendentes")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AtendenteController {

    private final AtendenteService atendenteService;

    /**
     * POST /api/atendentes
     * Cadastra um novo atendente.
     */
    @PostMapping
    public ResponseEntity<AtendenteResponse> cadastrar(
            @Valid @RequestBody CadastrarAtendenteRequest request) {

        log.info("Recebida requisição para cadastrar atendente: nome={}, time={}",
                request.getNome(), request.getTime());

        Atendente atendente = Atendente.builder()
                .nome(request.getNome())
                .time(request.getTime())
                .build();

        Atendente cadastrado = atendenteService.cadastrar(atendente);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AtendenteResponse.fromEntity(cadastrado));
    }

    /**
     * GET /api/atendentes
     * Lista todos os atendentes.
     */
    @GetMapping
    public ResponseEntity<List<AtendenteResponse>> listarTodos() {
        List<AtendenteResponse> atendentes = atendenteService.listarTodos()
                .stream()
                .map(AtendenteResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(atendentes);
    }

    /**
     * GET /api/atendentes/{id}
     * Busca um atendente por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AtendenteResponse> buscarPorId(@PathVariable Long id) {
        Atendente atendente = atendenteService.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Atendente não encontrado: " + id));

        return ResponseEntity.ok(AtendenteResponse.fromEntity(atendente));
    }

    /**
     * GET /api/atendentes/time/{time}
     * Lista atendentes de um time específico.
     */
    @GetMapping("/time/{time}")
    public ResponseEntity<List<AtendenteResponse>> listarPorTime(@PathVariable Time time) {
        List<AtendenteResponse> atendentes = atendenteService.listarPorTime(time)
                .stream()
                .map(AtendenteResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(atendentes);
    }

    /**
     * GET /api/atendentes/time/{time}/disponiveis
     * Lista apenas atendentes disponíveis de um time.
     */
    @GetMapping("/time/{time}/disponiveis")
    public ResponseEntity<List<AtendenteResponse>> listarDisponiveis(@PathVariable Time time) {
        List<AtendenteResponse> disponiveis = atendenteService.buscarDisponiveis(time)
                .stream()
                .map(AtendenteResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(disponiveis);
    }
}
