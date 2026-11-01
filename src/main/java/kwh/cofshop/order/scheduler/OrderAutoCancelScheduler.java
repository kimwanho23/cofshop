package kwh.cofshop.order.scheduler;

import kwh.cofshop.global.annotation.DistributedLock;
import kwh.cofshop.order.api.OrderPaymentReconcilePort;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.repository.OrderRepository;
import kwh.cofshop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderAutoCancelScheduler {

    private static final List<OrderState> AUTO_CANCEL_STATES =
            List.of(OrderState.WAITING_FOR_PAY, OrderState.PAYMENT_PENDING);

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final OrderPaymentReconcilePort orderPaymentReconcilePort;

    @Value("${order.auto-cancel.timeout-minutes:30}")
    private long timeoutMinutes;

    @Value("${order.auto-cancel.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${order.auto-cancel.fixed-delay-ms:60000}")
    @DistributedLock(
            keyName = "'order:auto-cancel:scheduler'",
            waitTime = 0, skipIfLocked = true,
            timeUnit = TimeUnit.MINUTES
    )
    public void autoCancelStaleOrders() {
        long safeTimeoutMinutes = Math.max(timeoutMinutes, 1L);
        int safeBatchSize = Math.max(batchSize, 1);
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(safeTimeoutMinutes);

        List<Long> staleOrderIds = orderRepository.findStaleOrderIdsForAutoCancel(
                AUTO_CANCEL_STATES,
                deadline,
                PageRequest.of(0, safeBatchSize)
        );
        if (staleOrderIds.isEmpty()) {
            return;
        }

        int cancelledCount = 0;
        for (Long orderId : staleOrderIds) {
            try {
                // 실패하면 결제 여부를 모르는 채로 취소하지 않고 다음 실행으로 넘김
                orderPaymentReconcilePort.reconcileBeforeCancel(orderId);
                if (orderService.autoCancelStaleUnpaidOrder(orderId, deadline)) {
                    cancelledCount++;
                }
            } catch (Exception e) {
                log.error("[OrderAutoCancel] 주문 자동 취소 실패 orderId={}", orderId, e);
            }
        }

        log.info("[OrderAutoCancel] 실행 완료 - candidates={}, cancelled={}",
                staleOrderIds.size(), cancelledCount);
    }
}
