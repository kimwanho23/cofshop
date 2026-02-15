package kwh.cofshop.coupon.messaging.logging.publisher;

import kwh.cofshop.coupon.messaging.CouponIssueEventPublisher;
import kwh.cofshop.coupon.messaging.CouponIssueEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(CouponIssueEventPublisher.class)
@Slf4j
public class LoggingCouponIssueEventPublisher implements CouponIssueEventPublisher {

    @Override
    public void publish(CouponIssueEventMessage message) {
        log.info("[CouponOutbox] publish placeholder eventId={}, memberCouponId={}, memberId={}, couponId={}",
                message.outboxEventId(), message.memberCouponId(), message.memberId(), message.couponId());
    }
}
