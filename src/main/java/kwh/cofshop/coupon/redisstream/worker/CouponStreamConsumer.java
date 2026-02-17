package kwh.cofshop.coupon.redisstream.worker;

import kwh.cofshop.coupon.domain.CouponIssueState;
import kwh.cofshop.coupon.redisstream.service.LimitedCouponIssueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "coupon.stream.enabled", havingValue = "true")
public class CouponStreamConsumer
        implements StreamListener<String, MapRecord<String, String, String>>, InitializingBean, DisposableBean {

    private final LimitedCouponIssueService limitedCouponIssueService;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            String memberIdValue = message.getValue().get("memberId");
            String couponIdValue = message.getValue().get("couponId");
            if (memberIdValue == null || couponIdValue == null) {
                throw new IllegalArgumentException("memberId/couponId is missing");
            }

            Long memberId = Long.valueOf(memberIdValue);
            Long couponId = Long.valueOf(couponIdValue);

            log.info("[LimitedCoupon] issue started, thread={}, id={}",
                    Thread.currentThread().getName(), Thread.currentThread().getId());

            CouponIssueState result = limitedCouponIssueService.issueCoupon(couponId, memberId);
            boolean terminalResult = true;

            switch (result) {
                case ALREADY_ISSUED -> log.warn("Duplicate issue request. memberId={}, couponId={}", memberId, couponId);
                case OUT_OF_STOCK -> log.warn("Out of stock. memberId={}, couponId={}", memberId, couponId);
                case SUCCESS -> log.info("Issue succeeded. memberId={}, couponId={}", memberId, couponId);
                case STOCK_NOT_INITIALIZED -> {
                    terminalResult = false;
                    log.warn("Stock is not initialized yet. memberId={}, couponId={}", memberId, couponId);
                }
            }

            if (terminalResult) {
                var ops = redisTemplate.opsForStream();
                ops.acknowledge(CouponStreamConstants.STREAM_KEY, CouponStreamConstants.COUPON_GROUP, message.getId());
                ops.delete(CouponStreamConstants.STREAM_KEY, message.getId());
            }
        } catch (Exception e) {
            // Leave message in pending to let cleaner handle retries/DLQ.
            log.error("Issue failed. payload={}, id={}", message.getValue(), message.getId(), e);
        }
    }

    @Override
    public void destroy() {
    }

    @Override
    public void afterPropertiesSet() {
    }
}
