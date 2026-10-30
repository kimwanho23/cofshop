package kwh.cofshop.coupon.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import kwh.cofshop.coupon.repository.outbox.CouponIssueOutboxRepository;
import kwh.cofshop.coupon.repository.outbox.entity.CouponIssueOutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponOutboxMetrics implements MeterBinder {

    private final CouponIssueOutboxRepository outboxRepository;

    @Override
    public void bindTo(MeterRegistry registry) {
        for (CouponIssueOutboxStatus status : new CouponIssueOutboxStatus[]{
                CouponIssueOutboxStatus.PENDING, CouponIssueOutboxStatus.FAILED}) {
            Gauge.builder("coupon.outbox.events", outboxRepository, repository -> repository.countByStatus(status))
                    .tag("status", status.name())
                    .description("쿠폰 발급 outbox 이벤트 건수")
                    .register(registry);
        }
    }
}
