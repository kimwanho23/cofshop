package kwh.cofshop.order.scheduler;

import kwh.cofshop.global.annotation.DistributedLock;
import kwh.cofshop.order.domain.OrderRefundRequestStatus;
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
public class OrderAutoConfirmScheduler {

    private static final List<OrderRefundRequestStatus> REFUND_IN_PROGRESS =
            List.of(OrderRefundRequestStatus.REQUESTED, OrderRefundRequestStatus.APPROVED);

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Value("${order.auto-confirm.after-days:7}")
    private long afterDays;

    @Value("${order.auto-confirm.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${order.auto-confirm.fixed-delay-ms:3600000}")
    @DistributedLock(keyName = "'order:auto-confirm:scheduler'", waitTime = 0, skipIfLocked = true, timeUnit = TimeUnit.MINUTES)
    public void autoConfirmDeliveredOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(Math.max(afterDays, 1L));
        List<Long> orderIds = orderRepository.findAutoConfirmTargets(
                OrderState.DELIVERED,
                deadline,
                REFUND_IN_PROGRESS,
                PageRequest.of(0, Math.max(batchSize, 1))
        );

        int confirmedCount = 0;
        for (Long orderId : orderIds) {
            try {
                if (orderService.autoConfirmDeliveredOrder(orderId, deadline)) {
                    confirmedCount++;
                }
            } catch (Exception e) {
                log.error("[OrderAutoConfirm] 자동 구매 확정 실패 orderId={}", orderId, e);
            }
        }

        if (!orderIds.isEmpty()) {
            log.info("[OrderAutoConfirm] 실행 완료 - candidates={}, confirmed={}", orderIds.size(), confirmedCount);
        }
    }
}
