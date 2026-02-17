package kwh.cofshop.coupon.scheduler;

import kwh.cofshop.coupon.service.CouponService;
import kwh.cofshop.coupon.service.MemberCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponScheduler {

    private static final int MAX_ATTEMPTS = 3;
    private static final int RETRY_DELAY_MS = 3000;

    private final CouponService couponService;
    private final MemberCouponService memberCouponService;

    @Async
    @Scheduled(cron = "0 0 0 * * *")
    public void expireCoupons() {
        try {
            expireAll();
        } catch (Exception e) {
            log.error("[Scheduler] Coupon expiration failed. Starting retries.", e);
            retryExpireCoupon();
        }
    }

    private void retryExpireCoupon() {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[Scheduler] Retry interrupted", e);
                return;
            }

            try {
                expireAll();
                log.info("[Scheduler] Coupon expiration retry succeeded (attempt={})", attempt);
                return;
            } catch (Exception e) {
                if (attempt == MAX_ATTEMPTS) {
                    log.error("[Scheduler] Coupon expiration retries exhausted (max={})", MAX_ATTEMPTS, e);
                } else {
                    log.warn("[Scheduler] Coupon expiration retry failed (attempt={}), retrying...", attempt, e);
                }
            }
        }
    }

    private void expireAll() {
        int expiredCoupons = couponService.expireCoupons(LocalDate.now());
        int expiredMemberCoupons = memberCouponService.expireMemberCoupons(LocalDate.now());
        log.info("[Scheduler] Coupon expiration completed - coupons: {}, memberCoupons: {}",
                expiredCoupons, expiredMemberCoupons);
    }
}
