package kwh.cofshop.order.scheduler;

import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.repository.OrderRepository;
import kwh.cofshop.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderAutoCancelSchedulerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderAutoCancelScheduler scheduler;

    @Test
    @DisplayName("주문 자동취소 스케줄러: 만료 주문이 없으면 아무 동작 안 함")
    void autoCancelStaleOrders_noCandidates() {
        ReflectionTestUtils.setField(scheduler, "timeoutMinutes", 30L);
        ReflectionTestUtils.setField(scheduler, "batchSize", 100);
        when(orderRepository.findStaleOrderIdsForAutoCancel(any(), any(), any(Pageable.class)))
                .thenReturn(List.of());

        scheduler.autoCancelStaleOrders();

        verify(orderService, never()).autoCancelStaleUnpaidOrder(any(), any());
    }

    @Test
    @DisplayName("주문 자동취소 스케줄러: 후보 주문 배치 처리")
    void autoCancelStaleOrders_processCandidates() {
        ReflectionTestUtils.setField(scheduler, "timeoutMinutes", 30L);
        ReflectionTestUtils.setField(scheduler, "batchSize", 2);
        when(orderRepository.findStaleOrderIdsForAutoCancel(any(), any(), any(Pageable.class)))
                .thenReturn(List.of(10L, 20L));
        when(orderService.autoCancelStaleUnpaidOrder(eq(10L), any(LocalDateTime.class))).thenReturn(true);
        when(orderService.autoCancelStaleUnpaidOrder(eq(20L), any(LocalDateTime.class))).thenReturn(false);

        scheduler.autoCancelStaleOrders();

        ArgumentCaptor<LocalDateTime> deadlineCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderService).autoCancelStaleUnpaidOrder(eq(10L), deadlineCaptor.capture());
        verify(orderService).autoCancelStaleUnpaidOrder(eq(20L), eq(deadlineCaptor.getValue()));
        assertThat(deadlineCaptor.getValue()).isBefore(LocalDateTime.now().minusMinutes(29));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderState>> stateCaptor = ArgumentCaptor.forClass(List.class);
        verify(orderRepository).findStaleOrderIdsForAutoCancel(
                stateCaptor.capture(),
                eq(deadlineCaptor.getValue()),
                any(Pageable.class)
        );
        assertThat(stateCaptor.getValue())
                .containsExactly(OrderState.WAITING_FOR_PAY, OrderState.PAYMENT_PENDING);
    }
}
