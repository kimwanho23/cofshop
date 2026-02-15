package kwh.cofshop.coupon.scheduler;

import kwh.cofshop.coupon.service.CouponService;
import kwh.cofshop.coupon.service.MemberCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponScheduler {

    private final CouponService couponService;
    private final MemberCouponService memberCouponService;

    private static final int MAX_ATTEMPTS = 3;
    private static final int RETRY_DELAY_MS = 3000; // 3�??��?

    // 쿠폰 만료 - ?�정???�행
    @Scheduled(cron = "0 0 0 * * *")
    public void expireCoupons() {
        try {
            expireAll();
        } catch (Exception e) {
            log.error("[Scheduler] 쿠폰 만료 ?�도 ?�패 - ?�시??.");
            retryExpireCoupon();
        }
    }

    // 쿠폰 만료 ?�시??
    private void retryExpireCoupon() {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[Scheduler] ?�시???��?�??�터?�트 발생", e);
                return;
            }

            try {
                expireAll();
                log.info("[Scheduler] 쿠폰 만료 ?�시???�공 ({}?�차)", attempt);
                return;
            } catch (Exception e) {
                if (attempt == MAX_ATTEMPTS) {
                    log.error("[Scheduler] 쿠폰 만료 ?�시???�패 - 최�? {}??초과", MAX_ATTEMPTS, e);
                } else {
                    log.warn("[Scheduler] 쿠폰 만료 ?�시???�패 ({}?�차), ?�시???�정...", attempt, e);
                }
            }
        }
    }

    // 쿠폰 만료 처리
    private void expireAll() {
        int expireCoupons = couponService.expireCoupons(LocalDate.now());// 쿠폰 ?�체 만료
        int expireMemberCoupons = memberCouponService.expireMemberCoupons(LocalDate.now());// 멤버?�의 쿠폰 ?�체 만료
        log.info("[Scheduler] Coupon expiration completed - coupons: {}, memberCoupons: {}", expireCoupons, expireMemberCoupons);
    }


}
