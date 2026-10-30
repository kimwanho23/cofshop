package kwh.cofshop.payment.scheduler;

import kwh.cofshop.global.annotation.DistributedLock;
import kwh.cofshop.payment.service.RefundCompensationRecoveryQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefundCompensationDeadLetterScheduler {

    private final RefundCompensationRecoveryQueueService recoveryQueueService;

    @Value("${payment.refund.recovery.dead-letter.batch-size:20}")
    private int batchSize;

    @Value("${payment.refund.recovery.dead-letter.sample-size:3}")
    private int sampleSize;

    @Scheduled(fixedDelayString = "${payment.refund.recovery.dead-letter.fixed-delay-ms:300000}")
    @DistributedLock(
            keyName = "'refund:compensation:dead-letter:monitor'",
            waitTime = 0,
            leaseTime = 5,
            timeUnit = TimeUnit.MINUTES
    )
    public void inspectDeadLetters() {
        long pendingBefore = recoveryQueueService.deadLetterSize();
        if (pendingBefore <= 0L) {
            return;
        }

        int safeBatchSize = Math.max(batchSize, 1);
        int safeSampleSize = Math.max(sampleSize, 1);

        int replayedCount = recoveryQueueService.replayDeadLetters(safeBatchSize);
        long pendingAfter = recoveryQueueService.deadLetterSize();
        List<String> samples = recoveryQueueService.peekDeadLetterPayloads(safeSampleSize)
                .stream()
                .map(this::abbreviate)
                .toList();

        log.error("[RefundRecoveryDLQ] pendingBefore={}, replayed={}, pendingAfter={}, samples={}",
                pendingBefore, replayedCount, pendingAfter, samples);
    }

    private String abbreviate(String payload) {
        if (payload == null) {
            return "null";
        }
        if (payload.length() <= 200) {
            return payload;
        }
        return payload.substring(0, 200) + "...";
    }
}
