package kwh.cofshop.coupon.event.Listener;


import kwh.cofshop.coupon.event.CouponCreatedEvent;
import kwh.cofshop.coupon.service.CouponRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponCreatedEventListener {

    private final CouponRedisService couponRedisService;

    @Async
    @EventListener
    public void handle(CouponCreatedEvent event) {
        if (event.isLimited()) { // 수량 제한이 있는가?
            try {
                couponRedisService.setInitCouponCount(event.couponId(), event.couponCount());
                log.info("[EventHandler] Redis 수량 초기화 완료: {}", event.couponId());
            } catch (Exception e) {
                log.error("[EventHandler] Redis 수량 초기화 실패: {}", event.couponId(), e);
            }
        }
    }
}
