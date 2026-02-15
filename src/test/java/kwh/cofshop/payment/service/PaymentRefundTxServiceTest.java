package kwh.cofshop.payment.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.service.OrderService;
import kwh.cofshop.payment.domain.PaymentEntity;
import kwh.cofshop.payment.domain.PaymentStatus;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentRefundTxServiceTest {

    @Mock
    private PaymentEntityRepository paymentEntityRepository;

    @Mock
    private OrderService orderService;

    @Mock
    private RefundCompensationRecoveryQueueService recoveryQueueService;

    @InjectMocks
    private PaymentRefundTxService paymentRefundTxService;

    @Test
    @DisplayName("로컬 환불 반영: 결제 정보 없음")
    void applyRefund_notFound() {
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentRefundTxService.applyRefund(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("로컬 환불 반영: 결제 상태가 PAID가 아님")
    void applyRefund_notPaid() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.READY);
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));

        assertThatThrownBy(() -> paymentRefundTxService.applyRefund(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("로컬 환불 반영: 성공")
    void applyRefund_success() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        Order order = mock(Order.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getOrder()).thenReturn(order);
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));

        paymentRefundTxService.applyRefund(1L, 1L);

        verify(paymentEntity).paymentStatusChange(PaymentStatus.CANCELLED);
        verify(orderService).cancelAndRestore(order);
    }

    @Test
    @DisplayName("환불 대기 전이: 결제/주문 상태 함께 변경")
    void markRefundPending_success() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        Order order = mock(Order.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getOrder()).thenReturn(order);
        when(order.getOrderState()).thenReturn(OrderState.PAID);
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));

        paymentRefundTxService.markRefundPending(1L, 1L);

        verify(paymentEntity).paymentStatusChange(PaymentStatus.REFUND_PENDING);
        verify(order).changeOrderState(OrderState.REFUND_PENDING);
    }

    @Test
    @DisplayName("환불 대기 롤백: 결제/주문 상태 복구")
    void rollbackRefundPending_success() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        Order order = mock(Order.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.REFUND_PENDING);
        when(paymentEntity.getOrder()).thenReturn(order);
        when(order.getOrderState()).thenReturn(OrderState.REFUND_PENDING);
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));

        paymentRefundTxService.rollbackRefundPending(1L, 1L);

        verify(paymentEntity).paymentStatusChange(PaymentStatus.PAID);
        verify(order).changeOrderState(OrderState.PAID);
    }

    @Test
    @DisplayName("로컬 환불 반영: 주문 취소 보정 충돌 시 예외 전파 및 복구 큐 적재")
    void applyRefund_orderCancelConflict() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        Order order = mock(Order.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getOrder()).thenReturn(order);
        when(order.getId()).thenReturn(10L);
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        doThrow(new BusinessException(BusinessErrorCode.ORDER_CANNOT_CANCEL))
                .when(orderService).cancelAndRestore(order);

        assertThatThrownBy(() -> paymentRefundTxService.applyRefund(1L, 1L))
                .isInstanceOf(BusinessException.class);

        verify(paymentEntity).paymentStatusChange(PaymentStatus.CANCELLED);
        verify(orderService).cancelAndRestore(order);
        verify(recoveryQueueService).enqueue(1L, 1L, 10L, BusinessErrorCode.ORDER_CANNOT_CANCEL.getCode());
    }
}
