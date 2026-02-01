package kwh.cofshop.coupon.scheduler;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.repository.CouponRepository;
import kwh.cofshop.coupon.service.CouponService;
import kwh.cofshop.coupon.service.MemberCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponScheduler {

    private final CouponService couponService;
    private final MemberCouponService memberCouponService;
    private final CouponRepository couponRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final int MAX_ATTEMPTS = 3;
    private static final int RETRY_DELAY_MS = 3000; // 3초 대기

    // 쿠폰 만료 - 자정에 실행
    @Scheduled(cron = "0 0 0 * * *")
    public void expireCoupons() {
        try {
            expireAll();
        } catch (Exception e) {
            log.error("[Scheduler] 쿠폰 만료 시도 실패 - 재시도..");
            retryExpireCoupon();
        }
    }

    // 쿠폰 만료 재시도
    private void retryExpireCoupon() {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                Thread.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[Scheduler] 재시도 대기 중 인터럽트 발생", e);
                return;
            }

            try {
                expireAll();
                log.info("[Scheduler] 쿠폰 만료 재시도 성공 ({}회차)", attempt);
                return;
            } catch (Exception e) {
                if (attempt == MAX_ATTEMPTS) {
                    log.error("[Scheduler] 쿠폰 만료 재시도 실패 - 최대 {}회 초과", MAX_ATTEMPTS, e);
                } else {
                    log.warn("[Scheduler] 쿠폰 만료 재시도 실패 ({}회차), 재시도 예정...", attempt, e);
                }
            }
        }
    }

    // 쿠폰 만료 처리
    private void expireAll() {
        int expireCoupons = couponService.expireCoupons(LocalDate.now());// 쿠폰 자체 만료
        int expireMemberCoupons = memberCouponService.expireMemberCoupons(LocalDate.now());// 멤버들의 쿠폰 전체 만료
        log.info("[Scheduler] 쿠폰 만료 성공- 쿠폰: {}건, 멤버 쿠폰: {}건", expireCoupons, expireMemberCoupons);
    }


    @Scheduled(cron = "0 0 0 * * *")
    public void syncCouponStock() {
        List<Coupon> coupons = couponRepository.findAll();

        for (Coupon coupon : coupons) {
            String redisKey = "coupon:stock:" + coupon.getId();

            String stock = redisTemplate.opsForValue().get(redisKey);
            if (stock != null) {
                int redisStock = Integer.parseInt(stock);
                coupon.updateCouponCount(redisStock);
            }
        }

        couponRepository.saveAll(coupons);
    }

}