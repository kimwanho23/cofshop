package kwh.cofshop.coupon.messaging.rabbit.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.coupon.messaging.CouponIssueEventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitMqCouponIssueEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private RabbitMqCouponIssueEventPublisher publisher;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(publisher, "exchange", "coupon.issue.exchange");
        ReflectionTestUtils.setField(publisher, "routingKey", "coupon.issue.created");
    }

    @Test
    @DisplayName("Ïø†Ìè∞ Î∞úÍ∏â ?¥Î≤§?∏Î? RabbitMQ exchange/routingKeyÎ°?Î∞úÌñâ")
    void publish() {
        CouponIssueEventMessage message = new CouponIssueEventMessage(10L, 11L, 1L, 5L);

        publisher.publish(message);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(
                eq("coupon.issue.exchange"),
                eq("coupon.issue.created"),
                payloadCaptor.capture()
        );
        assertThat(payloadCaptor.getValue()).contains("\"outboxEventId\":10");
        assertThat(payloadCaptor.getValue()).contains("\"memberCouponId\":11");
    }
}
