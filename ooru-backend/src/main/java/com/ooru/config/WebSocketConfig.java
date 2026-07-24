package com.ooru.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Replaces polling with a real push: when a booking's status changes, BookingService publishes to
 * "/topic/customer/{customerId}/bookings" and any connected client for that customer gets it
 * instantly, instead of hitting GET /api/bookings/mine every few seconds.
 *
 * SIMPLIFICATION WORTH KNOWING: the WebSocket handshake at /ws is left open (not JWT-checked) for
 * this MVP — Spring Security's HTTP filter chain doesn't naturally cover STOMP frames the way it
 * covers REST calls. A production version should authenticate the STOMP CONNECT frame (e.g. via a
 * ChannelInterceptor reading a token from connect headers) so a socket can't be opened by just
 * guessing someone else's customer id.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // tighten this to your real frontend origin before deploying
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
