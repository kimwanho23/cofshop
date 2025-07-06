package kwh.cofshop.coupon.worker;

import kwh.cofshop.coupon.service.CouponRedisService;
import kwh.cofshop.coupon.service.MemberCouponIssueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponStreamListener implements StreamListener<String, MapRecord<String, String, String>> {

    private final CouponRedisService couponRedisService;
    private final MemberCouponIssueService memberCouponIssueService;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        Long memberId = Long.valueOf(message.getValue().get("memberId"));
        Long couponId = Long.valueOf(message.getValue().get("couponId"));

        try {
            log.info("[LimitedCoupon] 스레드: {}, 쿠폰 발급 시작", Thread.currentThread().getName());

            // 1. 중복 발급 체크
            if (couponRedisService.isAlreadyIssued(couponId, memberId)) {
                log.warn("중복 발급 요청: memberId={}, couponId={}", memberId, couponId);
                return;
            }

            // 2. 재고 감소
            if (!couponRedisService.decreaseStock(couponId)) {
                log.warn("재고 없음: memberId={}, couponId={}", memberId, couponId);
                return;
            }

            Long saveData = memberCouponIssueService.issueLimitedCoupon(memberId, couponId);
            log.info("쿠폰 저장 - {}", saveData);

            // 5. 발급 기록 Redis에 저장
            couponRedisService.saveIssued(couponId, memberId);

            log.info("쿠폰 발급 성공: memberId={}, couponId={}", memberId, couponId);

        } catch (Exception e) {
            couponRedisService.restoreStock(couponId); // 예외 발생 시 재고 복구
            log.error("쿠폰 발급 실패: memberId={}, couponId={}", memberId, couponId, e);
        }
    }
}
