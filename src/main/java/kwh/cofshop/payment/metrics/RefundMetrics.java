package kwh.cofshop.payment.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import kwh.cofshop.order.api.OrderRefundPort;
import kwh.cofshop.payment.service.RefundCompensationRecoveryQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// 환불이 끝나지 못하고 쌓이는 건수. 수집 시점마다 Redis와 DB를 조회
@Component
@RequiredArgsConstructor
public class RefundMetrics implements MeterBinder {

    private static final long APPROVED_STUCK_MINUTES = 30L;

    private final RefundCompensationRecoveryQueueService recoveryQueueService;
    private final OrderRefundPort orderRefundPort;

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("refund.recovery.queue.size", recoveryQueueService,
                        RefundCompensationRecoveryQueueService::pendingSize)
                .description("환불 보상 복구 대기 건수")
                .register(registry);
        Gauge.builder("refund.recovery.dlq.size", recoveryQueueService,
                        RefundCompensationRecoveryQueueService::deadLetterSize)
                .description("환불 보상 복구 실패(DLQ) 건수")
                .register(registry);
        Gauge.builder("refund.approved.stuck", orderRefundPort,
                        port -> port.countPendingApprovedRefunds(LocalDateTime.now().minusMinutes(APPROVED_STUCK_MINUTES)))
                .description("승인 후 30분이 지나도 환불이 끝나지 않은 주문 건수")
                .register(registry);
    }
}
