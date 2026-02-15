package kwh.cofshop.coupon.infrastructure.messaging.rabbit.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.coupon.application.port.out.CouponIssueEventPublisher;
import kwh.cofshop.coupon.application.port.out.message.CouponIssueEventMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "coupon.rabbit.enabled", havingValue = "true")
public class RabbitMqCouponIssueEventPublisher implements CouponIssueEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${coupon.rabbit.exchange:coupon.issue.exchange}")
    private String exchange;

    @Value("${coupon.rabbit.routing-key:coupon.issue.created}")
    private String routingKey;

    @Override
    public void publish(CouponIssueEventMessage message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, serialize(message));
    }

    private String serialize(CouponIssueEventMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize coupon issue event", e);
        }
    }
}
