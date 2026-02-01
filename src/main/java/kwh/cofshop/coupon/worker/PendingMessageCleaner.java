package kwh.cofshop.coupon.worker;

import kwh.cofshop.coupon.domain.CouponIssueState;
import kwh.cofshop.coupon.service.CouponRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PendingMessageCleaner {

    private static final String CLEANER_CONSUMER = "pending-cleaner";

    private final StringRedisTemplate redisTemplate;
    private final CouponRedisService couponRedisService;

    @Value("${coupon.stream.pending.idle-ms:10000}")
    private long idleThresholdMs;

    @Value("${coupon.stream.pending.max-retries:5}")
    private int maxRetryCount;

    @Scheduled(fixedDelayString = "${coupon.stream.pending.fixed-delay-ms:1000}")
    public void cleanPendingMessages() {
        StreamOperations<String, String, String> ops = redisTemplate.opsForStream();
        PendingMessages pendingMessages = ops.pending(
                CouponStreamConstants.STREAM_KEY,
                CouponStreamConstants.COUPON_GROUP,
                Range.unbounded(),
                100
        );

        if (pendingMessages.isEmpty()) {
            return;
        }

        for (PendingMessage pending : pendingMessages) {
            long idleTime = pending.getElapsedTimeSinceLastDelivery().toMillis();
            if (idleTime < idleThresholdMs) {
                continue;
            }

            List<MapRecord<String, String, String>> claimed = ops.claim(
                    CouponStreamConstants.STREAM_KEY,
                    CouponStreamConstants.COUPON_GROUP,
                    CLEANER_CONSUMER,
                    Duration.ofMillis(idleThresholdMs),
                    pending.getId()
            );

            if (claimed.isEmpty()) {
                continue;
            }

            MapRecord<String, String, String> record = claimed.get(0);
            try {
                String memberIdValue = record.getValue().get("memberId");
                String couponIdValue = record.getValue().get("couponId");

                if (memberIdValue == null || couponIdValue == null) {
                    moveToDlq(ops, record, "missing_fields");
                    ackAndDelete(ops, record);
                    continue;
                }

                Long memberId = Long.valueOf(memberIdValue);
                Long couponId = Long.valueOf(couponIdValue);

                CouponIssueState result = couponRedisService.issueCoupon(couponId, memberId);

                switch (result) {
                    case SUCCESS -> {
                        ackAndDelete(ops, record);
                        log.info("[PendingCleaner] reprocess success id={}", record.getId());
                    }
                    case ALREADY_ISSUED, OUT_OF_STOCK -> {
                        ackAndDelete(ops, record);
                        log.warn("[PendingCleaner] terminal result={} id={}", result, record.getId());
                    }
                }
            } catch (Exception e) {
                if (pending.getTotalDeliveryCount() >= maxRetryCount) {
                    moveToDlq(ops, record, "retry_exceeded");
                    ackAndDelete(ops, record);
                } else {
                    log.warn("[PendingCleaner] reprocess failed id={}, deliveryCount={}, error={}",
                            record.getId(), pending.getTotalDeliveryCount(), e.getMessage());
                }
            }
        }
    }

    private void ackAndDelete(StreamOperations<String, String, String> ops, MapRecord<String, String, String> record) {
        ops.acknowledge(CouponStreamConstants.STREAM_KEY, CouponStreamConstants.COUPON_GROUP, record.getId());
        ops.delete(CouponStreamConstants.STREAM_KEY, record.getId());
    }

    private void moveToDlq(StreamOperations<String, String, String> ops,
                           MapRecord<String, String, String> record,
                           String reason) {
        Map<String, String> payload = Map.of(
                "memberId", record.getValue().getOrDefault("memberId", ""),
                "couponId", record.getValue().getOrDefault("couponId", ""),
                "originId", record.getId().getValue(),
                "reason", reason
        );
        ops.add(CouponStreamConstants.DLQ_STREAM_KEY, payload);
    }
}
