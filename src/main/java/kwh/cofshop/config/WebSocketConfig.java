package kwh.cofshop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat") // WebSocket 사용 시 연결할 엔드포인트
                .setAllowedOriginPatterns("http://localhost:8080") // http://localhost:8080/ws-chat 으로 보낸다.
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic"); // 클라이언트에게 메시지를 BroadCast할 때 사용할 prefix
        registry.setApplicationDestinationPrefixes("/app"); // 서버에 메시지를 전송할 때 사용할 prefix
    }
}
