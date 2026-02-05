package com.flowpay.atendimento.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

/**
 * Controller WebSocket para mensagens bidirecionais.
 * Este controller é opcional - usado principalmente para testes.
 */
@Controller
@Slf4j
public class WebSocketController {

    /**
     * Recebe mensagens do cliente em /app/ping
     * Responde em /topic/pong
     *
     * Útil para testar conexão WebSocket.
     */
    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public String handlePing(String message) {
        log.info("Recebido ping: {}", message);
        return "pong: " + message;
    }
}
