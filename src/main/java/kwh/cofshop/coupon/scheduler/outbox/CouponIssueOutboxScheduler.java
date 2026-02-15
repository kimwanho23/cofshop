package kwh.cofshop.coupon.scheduler.outbox;

import kwh.cofshop.coupon.service.outbox.CouponIssueOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "coupon.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class CouponIssueOutboxScheduler {

    private final CouponIssueOutboxService outboxService;

    @Value("${coupon.outbox.batch-size:100}")
    private int batchSize;

    @Value("${coupon.outbox.max-retries:5}")
    private int maxRetries;

    @Scheduled(fixedDelayString = "${coupon.outbox.fixed-delay-ms:2000}")
    public void publishPendingEvents() {
        int successCount = outboxService.publishPendingEvents(batchSize, maxRetries);
        if (successCount > 0) {
            log.info("[CouponOutbox] published events count={}", successCount);
        }
    }
}
