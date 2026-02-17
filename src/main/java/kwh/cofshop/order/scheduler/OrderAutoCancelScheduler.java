package kwh.cofshop.order.scheduler;

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

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderAutoCancelScheduler {

    private static final List<OrderState> AUTO_CANCEL_STATES =
            List.of(OrderState.WAITING_FOR_PAY, OrderState.PAYMENT_PENDING);

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Value("${order.auto-cancel.timeout-minutes:30}")
    private long timeoutMinutes;

    @Value("${order.auto-cancel.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${order.auto-cancel.fixed-delay-ms:60000}")
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
