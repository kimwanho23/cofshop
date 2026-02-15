package kwh.cofshop.coupon.infrastructure.legacy.redisstream.event;


import kwh.cofshop.coupon.domain.event.CouponCreatedEvent;
import kwh.cofshop.coupon.infrastructure.legacy.redisstream.service.CouponRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponCreatedEventListener {

    private final CouponRedisService couponRedisService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(CouponCreatedEvent event) {
        if (event.isLimited()) { // 수량 제한이 있는가?
            couponRedisService.setInitCouponCount(event.couponId(), event.couponCount());
            log.info("[EventHandler] Redis 수량 초기화 완료: {}", event.couponId());
        }
    }
}
