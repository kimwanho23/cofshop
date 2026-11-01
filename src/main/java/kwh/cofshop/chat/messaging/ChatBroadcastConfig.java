package kwh.cofshop.chat.messaging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

@Configuration
public class ChatBroadcastConfig {

    @Bean
    public RedisMessageListenerContainer chatBroadcastListenerContainer(
            RedisConnectionFactory connectionFactory,
            ChatBroadcaster chatBroadcaster
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                (message, pattern) -> chatBroadcaster.deliver(new String(message.getBody(), StandardCharsets.UTF_8)),
                new ChannelTopic(ChatBroadcaster.CHANNEL)
        );
        return container;
    }
}
