package kwh.cofshop.coupon.messaging.logging.publisher;

import kwh.cofshop.coupon.messaging.CouponIssueEventPublisher;
import kwh.cofshop.coupon.messaging.CouponIssueEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// 스캔 대상에 @ConditionalOnMissingBean을 쓰면 자기 자신이 조회되어 등록이 취소되므로 프로퍼티로 분기
@Component
@ConditionalOnProperty(name = "coupon.rabbit.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class LoggingCouponIssueEventPublisher implements CouponIssueEventPublisher {

    @Override
    public void publish(CouponIssueEventMessage message) {
        log.info("[CouponOutbox] publish placeholder eventId={}, memberCouponId={}, memberId={}, couponId={}",
                message.outboxEventId(), message.memberCouponId(), message.memberId(), message.couponId());
    }
}
