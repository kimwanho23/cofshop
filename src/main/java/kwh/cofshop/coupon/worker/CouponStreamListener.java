package kwh.cofshop.coupon.worker;

import kwh.cofshop.coupon.domain.CouponIssueState;
import kwh.cofshop.coupon.service.CouponRedisService;
import kwh.cofshop.coupon.service.MemberCouponIssueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponStreamListener implements StreamListener<String, MapRecord<String, String, String>> {

    private final CouponRedisService couponRedisService;
    private final MemberCouponIssueService memberCouponIssueService;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        Long memberId = Long.valueOf(message.getValue().get("memberId"));
        Long couponId = Long.valueOf(message.getValue().get("couponId"));

        boolean success = false;

        try {
            log.info("[LimitedCoupon] 스레드: {}, ID: {}, 쿠폰 발급 시작", Thread.currentThread().getName(), Thread.currentThread().getId());
            CouponIssueState result = couponRedisService.issueCoupon(couponId, memberId);

            switch (result) {
                case ALREADY_ISSUED -> log.warn("중복 발급 요청: memberId={}, couponId={}", memberId, couponId);
                case OUT_OF_STOCK -> log.warn("재고 부족: memberId={}, couponId={}", memberId, couponId);
                case SUCCESS -> {
                    Long savedId = memberCouponIssueService.issueLimitedCoupon(memberId, couponId);
                    log.info("쿠폰 발급 성공: memberId={}, couponId={}, savedId={}", memberId, couponId, savedId);
                    success = true;
                }
            }
            if (!success) {
                couponRedisService.restoreStock(couponId);
            }

            // 메시지 보냄
            redisTemplate.opsForStream()
                    .acknowledge("couponGroup", "coupon:issue:stream", message.getId());

        } catch (Exception e) {
            log.error("쿠폰 발급 실패: memberId={}, couponId={}", memberId, couponId, e);
        }
    }

}
