package kwh.cofshop.coupon.redisstream.worker;

import kwh.cofshop.coupon.domain.CouponIssueState;
import kwh.cofshop.coupon.redisstream.service.LimitedCouponIssueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CouponStreamConsumerTest {

    @Mock
    private LimitedCouponIssueService limitedCouponIssueService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @Test
    @DisplayName("?∞Î????ÅÌÉúÎ©?Î©îÏãúÏßÄÎ•?ACK ????†ú?úÎã§")
    @SuppressWarnings("unchecked")
    void onMessage_terminalResult_ack() {
        CouponStreamConsumer couponStreamConsumer = new CouponStreamConsumer(limitedCouponIssueService, redisTemplate);
        MapRecord<String, String, String> message = createMessage("1", "10", "1-0");
        when(redisTemplate.opsForStream()).thenReturn((StreamOperations) streamOperations);
        when(limitedCouponIssueService.issueCoupon(10L, 1L)).thenReturn(CouponIssueState.SUCCESS);

        couponStreamConsumer.onMessage(message);

        verify(streamOperations).acknowledge(
                CouponStreamConstants.STREAM_KEY,
                CouponStreamConstants.COUPON_GROUP,
                message.getId()
        );
        verify(streamOperations).delete(
                CouponStreamConstants.STREAM_KEY,
                message.getId()
        );
    }

    @Test
    @DisplayName("Stock not initialized should keep message pending")
    void onMessage_stockNotInitialized_noAck() {
        CouponStreamConsumer couponStreamConsumer = new CouponStreamConsumer(limitedCouponIssueService, redisTemplate);
        MapRecord<String, String, String> message = createMessage("1", "10", "2-0");
        when(limitedCouponIssueService.issueCoupon(10L, 1L)).thenReturn(CouponIssueState.STOCK_NOT_INITIALIZED);

        couponStreamConsumer.onMessage(message);

        verify(redisTemplate, never()).opsForStream();
    }

    @Test
    @DisplayName("payload ?ÑÎìú ?ÑÎùΩ?¥Î©¥ Î∞úÍ∏â ?∏Ï∂ú/ACK ?ÜÏù¥ Ï¢ÖÎ£å?úÎã§")
    void onMessage_missingField_noIssueNoAck() {
        CouponStreamConsumer couponStreamConsumer = new CouponStreamConsumer(limitedCouponIssueService, redisTemplate);
        MapRecord<String, String, String> message = createMessage(null, "10", "3-0");

        couponStreamConsumer.onMessage(message);

        verifyNoInteractions(limitedCouponIssueService);
        verify(redisTemplate, never()).opsForStream();
    }

    @SuppressWarnings("unchecked")
    private MapRecord<String, String, String> createMessage(String memberId, String couponId, String id) {
        MapRecord<String, String, String> message = org.mockito.Mockito.mock(MapRecord.class);
        if (memberId == null) {
            when(message.getValue()).thenReturn(Map.of("couponId", couponId));
        } else {
            when(message.getValue()).thenReturn(Map.of("memberId", memberId, "couponId", couponId));
        }
        when(message.getId()).thenReturn(RecordId.of(id));
        return message;
    }
}
