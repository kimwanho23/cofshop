package kwh.cofshop.coupon.worker;

import kwh.cofshop.coupon.domain.CouponIssueState;
import kwh.cofshop.coupon.service.CouponRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponStreamConsumer implements StreamListener<String, MapRecord<String, String, String>>, InitializingBean, DisposableBean {

    private final CouponRedisService couponRedisService;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        Long memberId = Long.valueOf(message.getValue().get("memberId"));
        Long couponId = Long.valueOf(message.getValue().get("couponId"));

        try {
            log.info("[LimitedCoupon] 발급 시작, thread={}, id={}",
                    Thread.currentThread().getName(), Thread.currentThread().getId());

            CouponIssueState result = couponRedisService.issueCoupon(couponId, memberId);

            switch (result) {
                case ALREADY_ISSUED -> log.warn("중복 발급 요청: memberId={}, couponId={}", memberId, couponId);
                case OUT_OF_STOCK -> log.warn("재고 부족: memberId={}, couponId={}", memberId, couponId);
                case SUCCESS -> log.info("발급 성공: memberId={}, couponId={}", memberId, couponId);
            }
            redisTemplate.opsForStream()
                    .acknowledge(CouponStreamConstants.STREAM_KEY, CouponStreamConstants.COUPON_GROUP, message.getId());

        } catch (Exception e) {
            // 예외 발생 시 pending 상태로 남겨두고 cleaner가 재시도
            log.error("발급 실패: memberId={}, couponId={}, id={}", memberId, couponId, message.getId(), e);
        }
    }

    @Override
    public void destroy() throws Exception {
    }

    @Override
    public void afterPropertiesSet() throws Exception {
    }
}
