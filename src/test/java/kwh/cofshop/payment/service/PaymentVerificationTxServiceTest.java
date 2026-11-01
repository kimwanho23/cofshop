package kwh.cofshop.payment.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.order.api.OrderStatePort;
import kwh.cofshop.order.api.OrderStatus;
import kwh.cofshop.payment.domain.PaymentEntity;
import kwh.cofshop.payment.domain.PaymentStatus;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentVerificationTxServiceTest {

    private static final Long PAYMENT_ID = 10L;
    private static final Long MEMBER_ID = 1L;
    private static final Long ORDER_ID = 100L;

    @Mock
    private PaymentEntityRepository paymentEntityRepository;

    @Mock
    private OrderStatePort orderStatePort;

    @InjectMocks
    private PaymentVerificationTxService service;

    @Test
    @DisplayName("결제 확정: 결제와 주문을 함께 완료 처리")
    void confirm_success() {
        PaymentEntity payment = given(ready(), OrderStatus.WAITING_FOR_PAY);

        service.confirmPaidPayment(PAYMENT_ID, MEMBER_ID, "imp", "pg", 1000L);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAmount()).isEqualTo(1000L);
        verify(orderStatePort).changeOrderState(ORDER_ID, OrderStatus.PAID);
    }

    @Test
    @DisplayName("결제 확정: 금액이 다르면 거부")
    void confirm_amountMismatch() {
        PaymentEntity payment = given(ready(), OrderStatus.WAITING_FOR_PAY);

        assertError(() -> service.confirmPaidPayment(PAYMENT_ID, MEMBER_ID, "imp", "pg", 999L),
                BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
    }

    @Test
    @DisplayName("결제 확정: 결제 대기가 아닌 주문은 거부")
    void confirm_orderNotWaiting() {
        given(ready(), OrderStatus.SHIPPING);

        assertError(() -> service.confirmPaidPayment(PAYMENT_ID, MEMBER_ID, "imp", "pg", 1000L),
                BusinessErrorCode.PAYMENT_FAIL);
        verify(orderStatePort, never()).changeOrderState(anyLong(), any());
    }

    @Test
    @DisplayName("결제 확정: 이미 취소된 결제는 거부")
    void confirm_alreadyCancelled() {
        given(cancelled(), OrderStatus.CANCELLED);

        assertError(() -> service.confirmPaidPayment(PAYMENT_ID, MEMBER_ID, "imp", "pg", 1000L),
                BusinessErrorCode.PAYMENT_FAIL);
    }

    @Test
    @DisplayName("결제 확정: 이미 완료된 결제의 재요청은 주문 상태만 맞추고 내용이 다르면 거부")
    void confirm_alreadyPaid() {
        given(paid(), OrderStatus.PAYMENT_PENDING);
        service.confirmPaidPayment(PAYMENT_ID, MEMBER_ID, "imp", "pg", 1000L);
        verify(orderStatePort).changeOrderState(ORDER_ID, OrderStatus.PAID);

        assertError(() -> service.confirmPaidPayment(PAYMENT_ID, MEMBER_ID, "other", "pg", 1000L),
                BusinessErrorCode.PAYMENT_UID_DISCREPANCY);
        assertError(() -> service.confirmPaidPayment(PAYMENT_ID, MEMBER_ID, "imp", "pg", 999L),
                BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY);
    }

    @Test
    @DisplayName("결제 확정: 이미 완료된 결제이고 주문도 진행 중이면 상태를 건드리지 않음")
    void confirm_alreadyPaid_orderProgressed() {
        given(paid(), OrderStatus.SHIPPING);

        service.confirmPaidPayment(PAYMENT_ID, MEMBER_ID, "imp", "pg", 1000L);

        verify(orderStatePort, never()).changeOrderState(anyLong(), any());
    }

    @Test
    @DisplayName("결제 확정: 주문 없는 결제는 거부")
    void confirm_orderMissing() {
        PaymentEntity payment = PaymentEntity.createPayment(
                null, MEMBER_ID, "merchant", 1000L, "a@example.com", "a", "01000000000", "kakaopay", "card");
        when(paymentEntityRepository.findByIdAndMemberIdWithLock(PAYMENT_ID, MEMBER_ID)).thenReturn(Optional.of(payment));

        assertError(() -> service.confirmPaidPayment(PAYMENT_ID, MEMBER_ID, "imp", "pg", 1000L),
                BusinessErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("결제 없음: 다른 회원의 결제도 같은 응답")
    void paymentNotFound() {
        when(paymentEntityRepository.findByIdAndMemberIdWithLock(PAYMENT_ID, MEMBER_ID)).thenReturn(Optional.empty());

        assertError(() -> service.confirmPaidPayment(PAYMENT_ID, MEMBER_ID, "imp", "pg", 1000L),
                BusinessErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("취소 주문 정리: 미결제 건은 결제 기록을 남기고 취소 처리, 금액이 다르면 거부")
    void settle_ready() {
        PaymentEntity payment = given(ready(), null);

        assertError(() -> service.settleCancelledOrderPayment(PAYMENT_ID, MEMBER_ID, "imp", "pg", 999L),
                BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY);

        service.settleCancelledOrderPayment(PAYMENT_ID, MEMBER_ID, "imp", "pg", 1000L);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(payment.getImpUid()).isEqualTo("imp");
    }

    @Test
    @DisplayName("취소 주문 정리: 완료 건은 내용이 같을 때만 취소 처리")
    void sync_paid() {
        PaymentEntity payment = given(paid(), null);

        assertError(() -> service.syncCancelledOrderPayment(PAYMENT_ID, MEMBER_ID, "other", "pg", 1000L),
                BusinessErrorCode.PAYMENT_UID_DISCREPANCY);

        service.syncCancelledOrderPayment(PAYMENT_ID, MEMBER_ID, "imp", "pg", 1000L);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    @DisplayName("취소 주문 정리: 이미 취소된 건의 재요청은 내용만 확인")
    void sync_alreadyCancelled() {
        PaymentEntity payment = given(cancelled(), null);

        service.syncCancelledOrderPayment(PAYMENT_ID, MEMBER_ID, "imp", "pg", 1000L);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);

        assertError(() -> service.syncCancelledOrderPayment(PAYMENT_ID, MEMBER_ID, "other", "pg", 1000L),
                BusinessErrorCode.PAYMENT_UID_DISCREPANCY);
        assertError(() -> service.syncCancelledOrderPayment(PAYMENT_ID, MEMBER_ID, "imp", "pg", 999L),
                BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY);
    }

    @Test
    @DisplayName("취소 주문 정리: 결제 기록이 없던 취소 건은 값 비교를 건너뜀")
    void sync_cancelledWithoutPaymentRecord() {
        PaymentEntity payment = ready();
        payment.paymentStatusChange(PaymentStatus.CANCELLED);
        given(payment, null);

        service.syncCancelledOrderPayment(PAYMENT_ID, MEMBER_ID, "imp", "pg", 1000L);

        assertThat(payment.getImpUid()).isNull();
    }

    private PaymentEntity given(PaymentEntity payment, OrderStatus orderStatus) {
        when(paymentEntityRepository.findByIdAndMemberIdWithLock(PAYMENT_ID, MEMBER_ID)).thenReturn(Optional.of(payment));
        if (orderStatus != null) {
            lenient().when(orderStatePort.getOrderState(ORDER_ID)).thenReturn(orderStatus);
        }
        return payment;
    }

    private PaymentEntity ready() {
        return PaymentEntity.createPayment(
                ORDER_ID, MEMBER_ID, "merchant", 1000L, "a@example.com", "a", "01000000000", "kakaopay", "card");
    }

    private PaymentEntity paid() {
        PaymentEntity payment = ready();
        payment.paymentSuccess("imp", "pg", 1000L, LocalDateTime.now());
        return payment;
    }

    private PaymentEntity cancelled() {
        PaymentEntity payment = paid();
        payment.paymentStatusChange(PaymentStatus.CANCELLED);
        return payment;
    }

    private void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, BusinessErrorCode expected) {
        assertThatThrownBy(action)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(expected);
    }
}
