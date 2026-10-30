package kwh.cofshop.payment.event;

import kwh.cofshop.order.api.OrderRefundApprovedEvent;
import kwh.cofshop.payment.service.ApprovedRefundExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderRefundApprovedEventListenerTest {

    @Mock
    private ApprovedRefundExecutor approvedRefundExecutor;

    @InjectMocks
    private OrderRefundApprovedEventListener listener;

    @Test
    @DisplayName("승인 이벤트: 환불 실행")
    void handle_executesRefund() {
        listener.handle(new OrderRefundApprovedEvent(1L));

        verify(approvedRefundExecutor).execute(1L);
    }

    @Test
    @DisplayName("승인 이벤트: 실패해도 예외를 전파하지 않음")
    void handle_swallowsFailure() {
        doThrow(new RuntimeException("pg down")).when(approvedRefundExecutor).execute(1L);

        assertThatCode(() -> listener.handle(new OrderRefundApprovedEvent(1L))).doesNotThrowAnyException();
    }
}
