package com.flowpay.atendimento.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuração do WebSocket para comunicação em tempo real.
 *
 * Endpoints disponíveis:
 * - ws://localhost:8080/ws - Conexão WebSocket
 * - /topic/* - Tópicos para broadcast (muitos clientes recebem)
 * - /queue/* - Filas para mensagens diretas (um cliente recebe)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Habilita broker simples em memória
        // Mensagens enviadas para /topic serão broadcast para todos os inscritos
        config.enableSimpleBroker("/topic", "/queue");

        // Prefixo para mensagens que vêm do cliente para o servidor
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Registra endpoint WebSocket
        // Clientes se conectam em: ws://localhost:8080/ws
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // Permite conexões de qualquer origem (Angular)
                .withSockJS();  // Fallback para navegadores sem suporte a WebSocket
    }
}
