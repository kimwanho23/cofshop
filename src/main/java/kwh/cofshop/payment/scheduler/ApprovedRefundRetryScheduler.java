package kwh.cofshop.payment.scheduler;

import kwh.cofshop.global.annotation.DistributedLock;
import kwh.cofshop.order.api.OrderRefundPort;
import kwh.cofshop.payment.service.ApprovedRefundExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovedRefundRetryScheduler {

    private final OrderRefundPort orderRefundPort;
    private final ApprovedRefundExecutor approvedRefundExecutor;

    @Value("${payment.refund.approved-retry.grace-minutes:5}")
    private long graceMinutes;

    @Value("${payment.refund.approved-retry.batch-size:20}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${payment.refund.approved-retry.fixed-delay-ms:300000}")
    @DistributedLock(keyName = "'refund:approved:retry'", waitTime = 0, skipIfLocked = true, timeUnit = TimeUnit.MINUTES)
    public void retryApprovedRefunds() {
        LocalDateTime approvedBefore = LocalDateTime.now().minusMinutes(Math.max(graceMinutes, 0L));
        List<Long> orderIds = orderRefundPort.findPendingApprovedRefundOrderIds(approvedBefore, Math.max(batchSize, 1));

        for (Long orderId : orderIds) {
            try {
                approvedRefundExecutor.execute(orderId);
                log.info("[ApprovedRefund] 재시도 성공 orderId={}", orderId);
            } catch (Exception e) {
                log.error("[ApprovedRefund] 재시도 실패 orderId={}", orderId, e);
            }
        }
    }
}
