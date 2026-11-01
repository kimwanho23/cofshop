package kwh.cofshop.coupon.scheduler.outbox;

import kwh.cofshop.global.annotation.DistributedLock;
import kwh.cofshop.coupon.service.outbox.CouponIssueOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "coupon.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class CouponIssueOutboxScheduler {

    private static final int CLEANUP_BATCH_SIZE = 1000;

    private final CouponIssueOutboxService outboxService;

    @Value("${coupon.outbox.batch-size:100}")
    private int batchSize;

    @Value("${coupon.outbox.max-retries:5}")
    private int maxRetries;

    @Value("${coupon.outbox.retention-days:7}")
    private int retentionDays;

    @DistributedLock(keyName = "'coupon:outbox:scheduler'", waitTime = 0, skipIfLocked = true)
    @Scheduled(fixedDelayString = "${coupon.outbox.fixed-delay-ms:2000}")
    public void publishPendingEvents() {
        int successCount = outboxService.publishPendingEvents(batchSize, maxRetries);
        if (successCount > 0) {
            log.info("[CouponOutbox] published events count={}", successCount);
        }
    }

    @DistributedLock(keyName = "'coupon:outbox:cleanup'", waitTime = 0, skipIfLocked = true)
    @Scheduled(cron = "${coupon.outbox.cleanup-cron:0 30 3 * * *}")
    public void deleteSentEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        int totalDeleted = 0;
        int deleted;
        do {
            // 한 번에 오래 잠그지 않도록 배치마다 커밋
            deleted = outboxService.deleteSentEventsBefore(cutoff, CLEANUP_BATCH_SIZE);
            totalDeleted += deleted;
        } while (deleted == CLEANUP_BATCH_SIZE);
        if (totalDeleted > 0) {
            log.info("[CouponOutbox] deleted sent events count={}", totalDeleted);
        }
    }
}
