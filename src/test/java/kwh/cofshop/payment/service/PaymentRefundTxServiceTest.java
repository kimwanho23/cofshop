package kwh.cofshop.payment.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.order.api.OrderCancellationPort;
import kwh.cofshop.order.api.OrderStatus;
import kwh.cofshop.order.api.OrderStatePort;
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
    private OrderCancellationPort orderCancellationPort;

    @Mock
    private OrderStatePort orderStatePort;

    @Mock
    private RefundCompensationRecoveryQueueService recoveryQueueService;

    @InjectMocks
    private PaymentRefundTxService paymentRefundTxService;

    @Test
    @DisplayName("confirmRefund_notFound")
    void confirmRefund_notFound() {
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentRefundTxService.confirmRefund(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("confirmRefund_notPaid")
    void confirmRefund_notPaid() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.READY);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));

        assertThatThrownBy(() -> paymentRefundTxService.confirmRefund(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("confirmRefund_success")
    void confirmRefund_success() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getOrderId()).thenReturn(10L);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));

        paymentRefundTxService.confirmRefund(1L, 1L);

        verify(paymentEntity).paymentStatusChange(PaymentStatus.CANCELLED);
        verify(orderCancellationPort).cancelAndRestore(10L);
    }

    @Test
    @DisplayName("markRefundPending_success")
    void markRefundPending_success() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getOrderId()).thenReturn(10L);
        when(orderStatePort.getOrderState(10L)).thenReturn(OrderStatus.PAID);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));

        paymentRefundTxService.markRefundPending(1L, 1L);

        verify(paymentEntity).paymentStatusChange(PaymentStatus.REFUND_PENDING);
        verify(orderStatePort).changeOrderState(10L, OrderStatus.REFUND_PENDING);
    }

    @Test
    @DisplayName("rollbackRefundPending_success")
    void rollbackRefundPending_success() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.REFUND_PENDING);
        when(paymentEntity.getOrderId()).thenReturn(10L);
        when(orderStatePort.getOrderState(10L)).thenReturn(OrderStatus.REFUND_PENDING);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));

        paymentRefundTxService.rollbackRefundPending(1L, 1L);

        verify(paymentEntity).paymentStatusChange(PaymentStatus.PAID);
        verify(orderStatePort).changeOrderState(10L, OrderStatus.PAID);
    }

    @Test
    @DisplayName("confirmRefund_orderCancelConflict")
    void confirmRefund_orderCancelConflict() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getOrderId()).thenReturn(10L);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        doThrow(new BusinessException(BusinessErrorCode.ORDER_CANNOT_CANCEL))
                .when(orderCancellationPort).cancelAndRestore(10L);

        assertThatThrownBy(() -> paymentRefundTxService.confirmRefund(1L, 1L))
                .isInstanceOf(BusinessException.class);

        verify(paymentEntity).paymentStatusChange(PaymentStatus.CANCELLED);
        verify(orderCancellationPort).cancelAndRestore(10L);
        verify(recoveryQueueService).enqueue(1L, 1L, 10L, BusinessErrorCode.ORDER_CANNOT_CANCEL.getCode());
    }
}



