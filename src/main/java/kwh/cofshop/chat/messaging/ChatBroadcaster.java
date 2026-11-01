package kwh.cofshop.chat.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

// 구독자가 다른 서버에 연결되어 있어도 받도록 Redis를 거쳐 모든 서버의 브로커로 전달
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatBroadcaster {

    public static final String CHANNEL = "chat:broadcast";

    private static final String DESTINATION_PREFIX = "/topic/chatroom.";
    private static final String ROOM_ID = "roomId";
    private static final String PAYLOAD = "payload";

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public void publish(Long roomId, Object payload) {
        try {
            redisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(Map.of(ROOM_ID, roomId, PAYLOAD, payload)));
        } catch (JsonProcessingException | RuntimeException e) {
            // 메시지는 이미 저장됨. Redis 장애 중에는 같은 서버의 구독자에게라도 전달
            log.warn("[ChatBroadcast] Redis 발행 실패, 이 서버에만 전달 roomId={}", roomId, e);
            messagingTemplate.convertAndSend(DESTINATION_PREFIX + roomId, payload);
        }
    }

    public void deliver(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            messagingTemplate.convertAndSend(DESTINATION_PREFIX + node.get(ROOM_ID).asLong(), node.get(PAYLOAD));
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("[ChatBroadcast] 메시지 전달 실패", e);
        }
    }
}
