package com.flowpay.atendimento.controller;

import com.flowpay.atendimento.dto.request.CriarAtendimentoRequest;
import com.flowpay.atendimento.dto.response.AtendimentoResponse;
import com.flowpay.atendimento.exception.RecursoNaoEncontradoException;
import com.flowpay.atendimento.model.Atendimento;
import com.flowpay.atendimento.model.StatusAtendimento;
import com.flowpay.atendimento.model.Time;
import com.flowpay.atendimento.service.AtendimentoService;
import com.flowpay.atendimento.service.DistribuidorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller REST para gerenciamento de atendimentos.
 */
@RestController
@RequestMapping("/api/atendimentos")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AtendimentoController {

    private final AtendimentoService atendimentoService;
    private final DistribuidorService distribuidorService;

    /**
     * POST /api/atendimentos
     * Cria um novo atendimento e o distribui automaticamente.
     */
    @PostMapping
    public ResponseEntity<AtendimentoResponse> criar(
            @Valid @RequestBody CriarAtendimentoRequest request) {

        log.info("Recebida requisição para criar atendimento: cliente={}, time={}",
                request.getNomeCliente(), request.getTime());

        Atendimento atendimento = Atendimento.builder()
                .nomeCliente(request.getNomeCliente())
                .assunto(request.getAssunto())
                .time(request.getTime())
                .build();

        Atendimento criado = atendimentoService.criar(atendimento);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AtendimentoResponse.fromEntity(criado));
    }

    /**
     * GET /api/atendimentos
     * Lista todos os atendimentos.
     */
    @GetMapping
    public ResponseEntity<List<AtendimentoResponse>> listarTodos() {
        List<AtendimentoResponse> atendimentos = atendimentoService.listarTodos()
                .stream()
                .map(AtendimentoResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(atendimentos);
    }

    /**
     * GET /api/atendimentos/{id}
     * Busca um atendimento por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AtendimentoResponse> buscarPorId(@PathVariable Long id) {
        Atendimento atendimento = atendimentoService.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Atendimento não encontrado: " + id));

        return ResponseEntity.ok(AtendimentoResponse.fromEntity(atendimento));
    }

    /**
     * GET /api/atendimentos/time/{time}
     * Lista atendimentos de um time específico.
     */
    @GetMapping("/time/{time}")
    public ResponseEntity<List<AtendimentoResponse>> listarPorTime(@PathVariable Time time) {
        List<AtendimentoResponse> atendimentos = atendimentoService.listarPorTime(time)
                .stream()
                .map(AtendimentoResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(atendimentos);
    }

    /**
     * GET /api/atendimentos/status/{status}
     * Lista atendimentos por status.
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<AtendimentoResponse>> listarPorStatus(
            @PathVariable StatusAtendimento status) {

        List<AtendimentoResponse> atendimentos = atendimentoService.listarPorStatus(status)
                .stream()
                .map(AtendimentoResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(atendimentos);
    }

    /**
     * PATCH /api/atendimentos/{id}/finalizar
     * Finaliza um atendimento.
     */
    @PatchMapping("/{id}/finalizar")
    public ResponseEntity<Void> finalizar(@PathVariable Long id) {
        log.info("Recebida requisição para finalizar atendimento: id={}", id);

        // Verifica se existe
        atendimentoService.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Atendimento não encontrado: " + id));

        distribuidorService.finalizarAtendimento(id);

        return ResponseEntity.noContent().build();
    }
}
