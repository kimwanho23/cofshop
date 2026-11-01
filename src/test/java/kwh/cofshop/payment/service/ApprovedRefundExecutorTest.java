package kwh.cofshop.payment.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.payment.domain.PaymentEntity;
import kwh.cofshop.payment.domain.PaymentStatus;
import kwh.cofshop.payment.dto.request.PaymentRefundRequestDto;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovedRefundExecutorTest {

    @Mock
    private PaymentEntityRepository paymentEntityRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentRefundTxService paymentRefundTxService;

    @InjectMocks
    private ApprovedRefundExecutor approvedRefundExecutor;

    @Test
    @DisplayName("승인 환불: 결제 회원과 실제 결제 금액으로 환불 실행")
    void execute_refundsPaidAmount() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getId()).thenReturn(10L);
        when(paymentEntity.getMemberId()).thenReturn(1L);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getPaidAmount()).thenReturn(1000L);
        when(paymentEntityRepository.findByOrderId(100L)).thenReturn(Optional.of(paymentEntity));

        approvedRefundExecutor.execute(100L);

        ArgumentCaptor<PaymentRefundRequestDto> captor = ArgumentCaptor.forClass(PaymentRefundRequestDto.class);
        verify(paymentService).refundPayment(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.eq(1L), captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("승인 환불: 결제가 이미 취소됐으면 주문 보정만 재시도")
    void execute_alreadyCancelled_confirmsOnly() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getId()).thenReturn(10L);
        when(paymentEntity.getMemberId()).thenReturn(1L);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.CANCELLED);
        when(paymentEntityRepository.findByOrderId(100L)).thenReturn(Optional.of(paymentEntity));

        approvedRefundExecutor.execute(100L);

        verify(paymentRefundTxService).confirmRefundForRecovery(10L, 1L);
        verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("승인 환불: 락 대기 중 다른 요청이 환불을 끝내면 주문 보정만 확인")
    void execute_cancelledWhileWaitingLock_confirmsOnly() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getId()).thenReturn(10L);
        when(paymentEntity.getMemberId()).thenReturn(1L);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getPaidAmount()).thenReturn(1000L);
        when(paymentEntityRepository.findByOrderId(100L)).thenReturn(Optional.of(paymentEntity));
        doThrow(new BusinessException(BusinessErrorCode.PAYMENT_ALREADY_CANCELLED))
                .when(paymentService).refundPayment(eq(10L), eq(1L), any());

        approvedRefundExecutor.execute(100L);

        verify(paymentRefundTxService).confirmRefundForRecovery(10L, 1L);
    }

    @Test
    @DisplayName("승인 환불: 다른 환불 실패는 재시도할 수 있게 전파")
    void execute_otherFailure_propagates() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getId()).thenReturn(10L);
        when(paymentEntity.getMemberId()).thenReturn(1L);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getPaidAmount()).thenReturn(1000L);
        when(paymentEntityRepository.findByOrderId(100L)).thenReturn(Optional.of(paymentEntity));
        doThrow(new BusinessException(BusinessErrorCode.PAYMENT_CANNOT_REFUND))
                .when(paymentService).refundPayment(eq(10L), eq(1L), any());

        assertThatThrownBy(() -> approvedRefundExecutor.execute(100L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.PAYMENT_CANNOT_REFUND);
        verifyNoInteractions(paymentRefundTxService);
    }

    @Test
    @DisplayName("승인 환불: 결제가 없으면 실패")
    void execute_paymentNotFound() {
        when(paymentEntityRepository.findByOrderId(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> approvedRefundExecutor.execute(100L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.PAYMENT_NOT_FOUND);
    }
}
