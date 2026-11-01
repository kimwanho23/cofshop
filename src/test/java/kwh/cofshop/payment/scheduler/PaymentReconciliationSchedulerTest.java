package kwh.cofshop.payment.scheduler;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.payment.domain.PaymentStatus;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import kwh.cofshop.payment.service.PaymentReconciliationTxService;
import kwh.cofshop.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReconciliationSchedulerTest {

    @Mock
    private PaymentEntityRepository paymentEntityRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentReconciliationTxService reconciliationTxService;

    @Mock
    private kwh.cofshop.payment.service.PaymentOperationLogService operationLogService;

    @InjectMocks
    private PaymentReconciliationScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "minAgeMinutes", 5L);
        ReflectionTestUtils.setField(scheduler, "maxAgeHours", 24L);
        ReflectionTestUtils.setField(scheduler, "batchSize", 50);
        when(paymentEntityRepository.findReconciliationTargets(eq(PaymentStatus.READY), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(1L, 2L));
    }

    @Test
    @DisplayName("대사: 한 건이 실패해도 다음 건을 처리하고 모두 확인 시각을 남김")
    void reconcilePayments_continuesAfterFailure() {
        doThrow(new BusinessException(BusinessErrorCode.PAYMENT_PROVIDER_ERROR))
                .when(paymentService).reconcilePayment(1L);

        scheduler.reconcilePayments();

        verify(paymentService).reconcilePayment(2L);
        verify(reconciliationTxService).markChecked(1L);
        verify(reconciliationTxService).markChecked(2L);
    }

    @Test
    @DisplayName("대사: 금액 불일치는 이력을 남기고 다음 건을 계속 처리")
    void reconcilePayments_mismatchHandled() {
        kwh.cofshop.payment.domain.PaymentEntity payment = org.mockito.Mockito.mock(kwh.cofshop.payment.domain.PaymentEntity.class);
        when(payment.getOrderId()).thenReturn(100L);
        when(paymentEntityRepository.findById(1L)).thenReturn(java.util.Optional.of(payment));
        doThrow(new BusinessException(BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY))
                .when(paymentService).reconcilePayment(1L);

        scheduler.reconcilePayments();

        verify(operationLogService).recordMismatchOnce(1L, 100L, BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY.getCode());
        verify(paymentService).reconcilePayment(2L);
    }
}
