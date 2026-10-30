package kwh.cofshop.payment.scheduler;

import kwh.cofshop.order.api.OrderRefundPort;
import kwh.cofshop.payment.service.ApprovedRefundExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovedRefundRetrySchedulerTest {

    @Mock
    private OrderRefundPort orderRefundPort;

    @Mock
    private ApprovedRefundExecutor approvedRefundExecutor;

    @InjectMocks
    private ApprovedRefundRetryScheduler scheduler;

    @Test
    @DisplayName("승인 환불 재시도: 한 건이 실패해도 다음 건을 계속 처리")
    void retryApprovedRefunds_continuesAfterFailure() {
        ReflectionTestUtils.setField(scheduler, "graceMinutes", 5L);
        ReflectionTestUtils.setField(scheduler, "batchSize", 20);
        when(orderRefundPort.findPendingApprovedRefundOrderIds(any(LocalDateTime.class), eq(20)))
                .thenReturn(List.of(1L, 2L));
        doThrow(new RuntimeException("pg down")).when(approvedRefundExecutor).execute(1L);

        scheduler.retryApprovedRefunds();

        verify(approvedRefundExecutor).execute(2L);
    }
}
