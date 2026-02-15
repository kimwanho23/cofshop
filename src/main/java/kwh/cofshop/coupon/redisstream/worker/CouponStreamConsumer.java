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
public class CouponStreamConsumer implements StreamListener<String, MapRecord<String, String, String>>, InitializingBean, DisposableBean {

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

            log.info("[LimitedCoupon] Î∞úÍ∏â ?úÏûë, thread={}, id={}",
                    Thread.currentThread().getName(), Thread.currentThread().getId());

            CouponIssueState result = limitedCouponIssueService.issueCoupon(couponId, memberId);
            boolean terminalResult = true;

            switch (result) {
                case ALREADY_ISSUED -> log.warn("Ï§ëÎ≥µ Î∞úÍ∏â ?îÏ≤≠: memberId={}, couponId={}", memberId, couponId);
                case OUT_OF_STOCK -> log.warn("?¨Í≥† Î∂ÄÏ°? memberId={}, couponId={}", memberId, couponId);
                case SUCCESS -> log.info("Î∞úÍ∏â ?±Í≥µ: memberId={}, couponId={}", memberId, couponId);
                case STOCK_NOT_INITIALIZED -> {
                    terminalResult = false;
                    log.warn("Redis ?¨Í≥† ÎØ∏Ï¥àÍ∏∞ÌôîÎ°??¨Ïãú???ÄÍ∏? memberId={}, couponId={}", memberId, couponId);
                }
            }

            if (terminalResult) {
                var ops = redisTemplate.opsForStream();
                ops.acknowledge(CouponStreamConstants.STREAM_KEY, CouponStreamConstants.COUPON_GROUP, message.getId());
                ops.delete(CouponStreamConstants.STREAM_KEY, message.getId());
            }

        } catch (Exception e) {
            // ?àÏô∏ Î∞úÏÉù ??pending ?ÅÌÉúÎ°??®Í≤®?êÍ≥† cleanerÍ∞Ä ?¨Ïãú??            log.error("Î∞úÍ∏â ?§Ìå®: payload={}, id={}", message.getValue(), message.getId(), e);
        }
    }

    @Override
    public void destroy() throws Exception {
    }

    @Override
    public void afterPropertiesSet() throws Exception {
    }
}
