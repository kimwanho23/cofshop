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

    // 매일 새벽 3시에 실행
    @Scheduled(cron = "0 0 0 * * *")
    public void expireCoupons() {
        couponService.expireCoupons(LocalDate.now()); // 쿠폰 자체 만료
        memberCouponService.expireMemberCoupons(LocalDate.now()); // 멤버들의 쿠폰 전체 만료
        log.info("[Scheduler] : 쿠폰 만료 처리");
    }
}