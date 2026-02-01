package kwh.cofshop.coupon.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

import java.util.Map;

import static kwh.cofshop.coupon.worker.CouponStreamConstants.STREAM_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberCouponRedisServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @InjectMocks
    private MemberCouponRedisService memberCouponRedisService;

    @Test
    @DisplayName("레디스 스트림 발급 요청 적재")
    void enqueueLimitedCouponIssueRequest() {
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);

        memberCouponRedisService.enqueueLimitedCouponIssueRequest(1L, 10L);

        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(streamOperations).add(org.mockito.ArgumentMatchers.eq(STREAM_KEY), captor.capture());
        Map<String, String> payload = captor.getValue();
        assertThat(payload.get("memberId")).isEqualTo("1");
        assertThat(payload.get("couponId")).isEqualTo("10");
    }
}