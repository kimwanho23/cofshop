package kwh.cofshop.coupon.redisstream.event;


import kwh.cofshop.coupon.domain.event.CouponCreatedEvent;
import kwh.cofshop.coupon.redisstream.service.CouponRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "coupon.stream.enabled", havingValue = "true")
public class CouponCreatedEventListener {

    private final CouponRedisService couponRedisService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(CouponCreatedEvent event) {
        if (event.isLimited()) { // ?˜ëŸ‰ ?œí•œ???ˆëŠ”ê°€?
            couponRedisService.setInitCouponCount(event.couponId(), event.couponCount());
            log.info("[EventHandler] Redis ?˜ëŸ‰ ì´ˆê¸°???„ë£Œ: {}", event.couponId());
        }
    }
}
