package kwh.cofshop.payment.event;

import kwh.cofshop.order.api.OrderRefundApprovedEvent;
import kwh.cofshop.payment.service.ApprovedRefundExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderRefundApprovedEventListener {

    private final ApprovedRefundExecutor approvedRefundExecutor;

    // 승인 커밋 후 별도 스레드에서 실행. 실패하면 ApprovedRefundRetryScheduler가 재시도
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderRefundApprovedEvent event) {
        try {
            approvedRefundExecutor.execute(event.orderId());
        } catch (Exception e) {
            log.error("[ApprovedRefund] 승인 환불 실행 실패 orderId={}", event.orderId(), e);
        }
    }
}
