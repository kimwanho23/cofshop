package kwh.cofshop.coupon.infrastructure.messaging.logging.publisher;

import kwh.cofshop.coupon.application.port.out.CouponIssueEventPublisher;
import kwh.cofshop.coupon.application.port.out.message.CouponIssueEventMessage;
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
