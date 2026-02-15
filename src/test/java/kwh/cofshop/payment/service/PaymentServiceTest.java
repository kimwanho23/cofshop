package kwh.cofshop.payment.service;

import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.order.api.OrderPaymentPrepareInfo;
import kwh.cofshop.order.api.OrderPaymentPreparePort;
import kwh.cofshop.order.api.OrderStatus;
import kwh.cofshop.order.api.OrderStatePort;
import kwh.cofshop.payment.client.portone.PortOneCancellation;
import kwh.cofshop.payment.client.portone.PortOnePayment;
import kwh.cofshop.payment.domain.PaymentEntity;
import kwh.cofshop.payment.domain.PaymentStatus;
import kwh.cofshop.payment.dto.request.PaymentPrepareRequestDto;
import kwh.cofshop.payment.dto.response.PaymentProviderResponseDto;
import kwh.cofshop.payment.dto.request.PaymentRefundRequestDto;
import kwh.cofshop.payment.dto.response.PaymentResponseDto;
import kwh.cofshop.payment.dto.request.PaymentVerifyRequestDto;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderPaymentPreparePort orderPaymentPreparePort;

    @Mock
    private OrderStatePort orderStatePort;

    @Mock
    private PaymentEntityRepository paymentEntityRepository;

    @Mock
    private PaymentProviderService paymentProviderService;

    @Mock
    private PaymentRefundTxService paymentRefundTxService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("getPaymentByImpUid_invalid")
    void getPaymentByImpUid_invalid() {
        assertThatThrownBy(() -> paymentService.getPaymentByImpUid(1L, " "))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("getPaymentByImpUid_error")
    void getPaymentByImpUid_error() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByImpUidAndMemberId("imp_1", 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentProviderService.getPayment("order-1")).thenThrow(new BusinessException(BusinessErrorCode.PAYMENT_PROVIDER_ERROR));

        assertThatThrownBy(() -> paymentService.getPaymentByImpUid(1L, "imp_1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getPaymentByImpUid_uidMismatch")
    void getPaymentByImpUid_uidMismatch() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByImpUidAndMemberId("imp_1", 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");

        PortOnePayment payment = new PortOnePayment(
                "order-1",
                "imp_2",
                "pg_tid",
                "PAID",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(paymentProviderService.getPayment("order-1")).thenReturn(payment);

        assertThatThrownBy(() -> paymentService.getPaymentByImpUid(1L, "imp_1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getPaymentByImpUid_success")
    void getPaymentByImpUid_success() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByImpUidAndMemberId("imp_1", 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");

        PortOnePayment payment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "PAID",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(paymentProviderService.getPayment("order-1")).thenReturn(payment);

        PaymentProviderResponseDto result = paymentService.getPaymentByImpUid(1L, "imp_1");

        assertThat(result.getPaymentId()).isEqualTo("order-1");
        assertThat(result.getTransactionId()).isEqualTo("imp_1");
        assertThat(result.getPgTxId()).isEqualTo("pg_tid");
        assertThat(result.getPaidAmount()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("createPaymentRequest_notFound")
    void createPaymentRequest_notFound() {
        when(orderPaymentPreparePort.prepare(1L, 1L))
                .thenThrow(new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND));

        PaymentPrepareRequestDto requestDto = PaymentPrepareRequestDto.builder()
                .pgProvider("kakaopay")
                .payMethod("card")
                .build();

        assertThatThrownBy(() -> paymentService.createPaymentRequest(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("createPaymentRequest_success")
    void createPaymentRequest_success() {
        OrderPaymentPrepareInfo paymentPrepareInfo = new OrderPaymentPrepareInfo(
                1L,
                "order-1",
                1000L,
                1L,
                "test@test.com",
                "tester",
                "010-0000-0000"
        );
        when(orderPaymentPreparePort.prepare(1L, 1L)).thenReturn(paymentPrepareInfo);
        when(paymentEntityRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentPrepareRequestDto requestDto = PaymentPrepareRequestDto.builder()
                .pgProvider("kakaopay")
                .payMethod("card")
                .build();

        PaymentResponseDto response = paymentService.createPaymentRequest(1L, 1L, requestDto);

        assertThat(response.getPgProvider()).isEqualTo("kakaopay");
        assertThat(response.getPayMethod()).isEqualTo("card");
        verify(orderPaymentPreparePort).prepare(1L, 1L);
    }

    @Test
    @DisplayName("verifyPayment_notFound")
    void verifyPayment_notFound() {
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.empty());

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.verifyPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("verifyPayment_portOneError")
    void verifyPayment_portOneError() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentProviderService.getPayment("order-1")).thenThrow(new BusinessException(BusinessErrorCode.PAYMENT_PROVIDER_ERROR));

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.verifyPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("verifyPayment_uidMismatch")
    void verifyPayment_uidMismatch() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));

        PortOnePayment payment = new PortOnePayment(
                "order-2",
                "imp_1",
                "pg_tid",
                "PAID",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(paymentProviderService.getPayment("order-1")).thenReturn(payment);

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.verifyPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("verifyPayment_notPaidStatus")
    void verifyPayment_notPaidStatus() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");

        PortOnePayment payment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "READY",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(paymentProviderService.getPayment("order-1")).thenReturn(payment);

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.verifyPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("verifyPayment_amountMismatch")
    void verifyPayment_amountMismatch() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");

        PortOnePayment payment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "PAID",
                new PortOnePayment.Amount(1000L, 900L)
        );
        when(paymentProviderService.getPayment("order-1")).thenReturn(payment);

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.verifyPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("verifyPayment_success")
    void verifyPayment_success() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntity.getPrice()).thenReturn(1000L);
        when(paymentEntity.getOrderId()).thenReturn(1L);
        when(orderStatePort.getOrderState(1L)).thenReturn(OrderStatus.PAYMENT_PENDING);

        PortOnePayment payment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "PAID",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(paymentProviderService.getPayment("order-1")).thenReturn(payment);

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        paymentService.verifyPayment(1L, 1L, requestDto);

        verify(paymentEntity).paymentSuccess(eq("imp_1"), eq("pg_tid"), eq(1000L), any(LocalDateTime.class));
        verify(orderStatePort).changeOrderState(1L, OrderStatus.PAID);
    }

    @Test
    @DisplayName("verifyPayment_cancelledOrder_shouldFail")
    void verifyPayment_cancelledOrder_shouldFail() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.READY);
        when(paymentEntity.getPrice()).thenReturn(1000L);
        when(paymentEntity.getOrderId()).thenReturn(1L);
        when(orderStatePort.getOrderState(1L)).thenReturn(OrderStatus.CANCELLED);

        PortOnePayment payment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "PAID",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(paymentProviderService.getPayment("order-1")).thenReturn(payment);

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.verifyPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.ORDER_ALREADY_CANCELLED);

        verify(paymentEntity, never()).paymentSuccess(anyString(), anyString(), anyLong(), any(LocalDateTime.class));
        verify(orderStatePort, never()).changeOrderState(1L, OrderStatus.PAID);
    }

    @Test
    @DisplayName("verifyPayment_alreadyPaid_shouldNotDowngradeOrderStatus")
    void verifyPayment_alreadyPaid_shouldNotDowngradeOrderStatus() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getImpUid()).thenReturn("imp_1");
        when(paymentEntity.getPaidAmount()).thenReturn(1000L);
        when(paymentEntity.getOrderId()).thenReturn(1L);
        when(orderStatePort.getOrderState(1L)).thenReturn(OrderStatus.DELIVERED);

        PortOnePayment payment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "PAID",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(paymentProviderService.getPayment("order-1")).thenReturn(payment);

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        paymentService.verifyPayment(1L, 1L, requestDto);

        verify(paymentEntity, never()).paymentSuccess(anyString(), anyString(), anyLong(), any(LocalDateTime.class));
        verify(orderStatePort, never()).changeOrderState(1L, OrderStatus.PAID);
    }

    @Test
    @DisplayName("verifyPayment_alreadyPaid_pendingOrder_thenSetPaid")
    void verifyPayment_alreadyPaid_pendingOrder_thenSetPaid() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getImpUid()).thenReturn("imp_1");
        when(paymentEntity.getPaidAmount()).thenReturn(1000L);
        when(paymentEntity.getOrderId()).thenReturn(1L);
        when(orderStatePort.getOrderState(1L)).thenReturn(OrderStatus.PAYMENT_PENDING);

        PortOnePayment payment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "PAID",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(paymentProviderService.getPayment("order-1")).thenReturn(payment);

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        paymentService.verifyPayment(1L, 1L, requestDto);

        verify(orderStatePort).changeOrderState(1L, OrderStatus.PAID);
    }

    @Test
    @DisplayName("refundPayment_alreadyCancelled")
    void refundPayment_alreadyCancelled() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.CANCELLED);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));

        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.refundPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("refundPayment_notPaid")
    void refundPayment_notPaid() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.READY);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));

        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.refundPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("refundPayment_pending_thenConfirm")
    void refundPayment_pending_thenConfirm() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.REFUND_PENDING);
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));

        PortOnePayment payment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "CANCELLED",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(paymentProviderService.getPayment("order-1")).thenReturn(payment);

        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(1000L);

        paymentService.refundPayment(1L, 1L, requestDto);

        verify(paymentRefundTxService).confirmRefund(1L, 1L);
    }

    @Test
    @DisplayName("refundPayment_portOneError")
    void refundPayment_portOneError() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getPaidAmount()).thenReturn(1000L);
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntity.getOrderId()).thenReturn(1L);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(orderStatePort.getOrderState(1L)).thenReturn(OrderStatus.PAID);
        when(paymentProviderService.cancelPayment("order-1")).thenThrow(new BusinessException(BusinessErrorCode.PAYMENT_PROVIDER_ERROR));
        when(paymentProviderService.isPaymentCancelled("order-1")).thenReturn(false);

        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.refundPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);

        verify(paymentRefundTxService).markRefundPending(1L, 1L);
        verify(paymentRefundTxService).rollbackRefundPending(1L, 1L);
    }

    @Test
    @DisplayName("refundPayment_failed")
    void refundPayment_failed() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getPaidAmount()).thenReturn(1000L);
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntity.getOrderId()).thenReturn(1L);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(orderStatePort.getOrderState(1L)).thenReturn(OrderStatus.PAID);

        PortOneCancellation cancellation = new PortOneCancellation("cancel_1", "FAILED");
        when(paymentProviderService.cancelPayment("order-1")).thenReturn(cancellation);

        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.refundPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);

        verify(paymentRefundTxService).markRefundPending(1L, 1L);
        verify(paymentRefundTxService).rollbackRefundPending(1L, 1L);
    }

    @Test
    @DisplayName("refundPayment_statusNotCancelled")
    void refundPayment_statusNotCancelled() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getPaidAmount()).thenReturn(1000L);
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntity.getOrderId()).thenReturn(1L);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(orderStatePort.getOrderState(1L)).thenReturn(OrderStatus.PAID);

        PortOneCancellation cancellation = new PortOneCancellation("cancel_1", "SUCCEEDED");
        when(paymentProviderService.cancelPayment("order-1")).thenReturn(cancellation);
        PortOnePayment payment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "PAID",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(paymentProviderService.getPayment("order-1")).thenReturn(payment);

        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.refundPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);

        verify(paymentRefundTxService).markRefundPending(1L, 1L);
        verify(paymentRefundTxService).rollbackRefundPending(1L, 1L);
    }

    @Test
    @DisplayName("refundPayment_success")
    void refundPayment_success() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getPaidAmount()).thenReturn(1000L);
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntity.getOrderId()).thenReturn(1L);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(orderStatePort.getOrderState(1L)).thenReturn(OrderStatus.PAID);

        PortOneCancellation cancellation = new PortOneCancellation("cancel_1", "SUCCEEDED");
        when(paymentProviderService.cancelPayment("order-1")).thenReturn(cancellation);
        PortOnePayment payment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "CANCELLED",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(paymentProviderService.getPayment("order-1")).thenReturn(payment);

        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(1000L);

        paymentService.refundPayment(1L, 1L, requestDto);

        verify(paymentRefundTxService).markRefundPending(1L, 1L);
        verify(paymentRefundTxService).confirmRefund(1L, 1L);
    }

    @Test
    @DisplayName("refundPayment_orderStateNotPaid")
    void refundPayment_orderStateNotPaid() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getPaidAmount()).thenReturn(1000L);
        when(paymentEntity.getOrderId()).thenReturn(1L);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(orderStatePort.getOrderState(1L)).thenReturn(OrderStatus.DELIVERED);

        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.refundPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

}




