/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

/**
 * Config for websocket communication.
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {
    @Override
    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/partypics")
    }

    @Override
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws").setAllowedOrigins("*")
        registry.addEndpoint("/ws").setAllowedOrigins("*").withSockJS()
    }
}
