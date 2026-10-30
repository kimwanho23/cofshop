package kwh.cofshop.payment.service;

import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.order.api.OrderPaymentPrepareInfo;
import kwh.cofshop.order.api.OrderPaymentPreparePort;
import kwh.cofshop.order.api.OrderRefundPort;
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
import kwh.cofshop.payment.repository.projection.PaymentProviderLookupProjection;
import kwh.cofshop.payment.service.PaymentService.PaymentCreateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
    private OrderRefundPort orderRefundPort;

    @Mock
    private PaymentEntityRepository paymentEntityRepository;

    @Mock
    private PaymentProviderService paymentProviderService;

    @Mock
    private PaymentRefundTxService paymentRefundTxService;

    @Mock
    private PaymentVerificationTxService paymentVerificationTxService;

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
        PaymentProviderLookupProjection paymentLookup = mock(PaymentProviderLookupProjection.class);
        when(paymentEntityRepository.findByImpUidAndMemberId("imp_1", 1L))
                .thenReturn(Optional.of(paymentLookup));
        when(paymentLookup.getMerchantUid()).thenReturn("order-1");
        when(paymentProviderService.getPayment("order-1")).thenThrow(new BusinessException(BusinessErrorCode.PAYMENT_PROVIDER_ERROR));

        assertThatThrownBy(() -> paymentService.getPaymentByImpUid(1L, "imp_1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getPaymentByImpUid_uidMismatch")
    void getPaymentByImpUid_uidMismatch() {
        PaymentProviderLookupProjection paymentLookup = mock(PaymentProviderLookupProjection.class);
        when(paymentEntityRepository.findByImpUidAndMemberId("imp_1", 1L))
                .thenReturn(Optional.of(paymentLookup));
        when(paymentLookup.getMerchantUid()).thenReturn("order-1");

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
        PaymentProviderLookupProjection paymentLookup = mock(PaymentProviderLookupProjection.class);
        when(paymentEntityRepository.findByImpUidAndMemberId("imp_1", 1L))
                .thenReturn(Optional.of(paymentLookup));
        when(paymentLookup.getMerchantUid()).thenReturn("order-1");

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

        PaymentPrepareRequestDto requestDto = new PaymentPrepareRequestDto("kakaopay", "card");

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

        PaymentPrepareRequestDto requestDto = new PaymentPrepareRequestDto("kakaopay", "card");

        PaymentCreateResult result = paymentService.createPaymentRequest(1L, 1L, requestDto);
        PaymentResponseDto response = result.response();

        assertThat(response.getPgProvider()).isEqualTo("kakaopay");
        assertThat(response.getPayMethod()).isEqualTo("card");
        assertThat(result.created()).isTrue();
        verify(orderPaymentPreparePort).prepare(1L, 1L);
    }

    @Test
    @DisplayName("createPaymentRequest_existingPayment_returnsExisting")
    void createPaymentRequest_existingPayment_returnsExisting() {
        PaymentEntity existingPayment = mock(PaymentEntity.class);
        when(existingPayment.getMemberId()).thenReturn(1L);
        when(existingPayment.getId()).thenReturn(10L);
        when(existingPayment.getMerchantUid()).thenReturn("order-1");
        when(existingPayment.getPrice()).thenReturn(1000L);
        when(existingPayment.getPgProvider()).thenReturn("kakaopay");
        when(existingPayment.getPayMethod()).thenReturn("card");
        when(existingPayment.getStatus()).thenReturn(PaymentStatus.READY);
        when(paymentEntityRepository.findByOrderId(1L)).thenReturn(Optional.of(existingPayment));

        PaymentPrepareRequestDto requestDto = new PaymentPrepareRequestDto("kakaopay", "card");

        PaymentCreateResult result = paymentService.createPaymentRequest(1L, 1L, requestDto);
        PaymentResponseDto response = result.response();

        assertThat(response.getPaymentId()).isEqualTo(10L);
        assertThat(result.created()).isFalse();
        verify(orderPaymentPreparePort, never()).prepare(anyLong(), anyLong());
        verify(paymentEntityRepository, never()).save(any(PaymentEntity.class));
    }

    @Test
    @DisplayName("createPaymentRequest_duplicateInsert_returnsExisting")
    void createPaymentRequest_duplicateInsert_returnsExisting() {
        OrderPaymentPrepareInfo paymentPrepareInfo = new OrderPaymentPrepareInfo(
                1L,
                "order-1",
                1000L,
                1L,
                "test@test.com",
                "tester",
                "010-0000-0000"
        );
        when(paymentEntityRepository.findByOrderId(1L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(mockDuplicatedPayment(1L, 11L)));
        when(orderPaymentPreparePort.prepare(1L, 1L)).thenReturn(paymentPrepareInfo);
        when(paymentEntityRepository.save(any(PaymentEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        PaymentPrepareRequestDto requestDto = new PaymentPrepareRequestDto("kakaopay", "card");

        PaymentCreateResult result = paymentService.createPaymentRequest(1L, 1L, requestDto);
        PaymentResponseDto response = result.response();

        assertThat(response.getPaymentId()).isEqualTo(11L);
        assertThat(response.getMerchantUid()).isEqualTo("order-1");
        assertThat(result.created()).isFalse();
        verify(orderPaymentPreparePort).prepare(1L, 1L);
    }

    @Test
    @DisplayName("createPaymentRequest_invalidPayMethod")
    void createPaymentRequest_invalidPayMethod() {
        PaymentPrepareRequestDto requestDto = new PaymentPrepareRequestDto("kakaopay", "bad_method");

        assertThatThrownBy(() -> paymentService.createPaymentRequest(1L, 1L, requestDto))
                .isInstanceOf(BadRequestException.class);

        verify(orderPaymentPreparePort, never()).prepare(anyLong(), anyLong());
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

        verify(paymentVerificationTxService)
                .confirmPaidPayment(1L, 1L, "imp_1", "pg_tid", 1000L);
    }

    @Test
    @DisplayName("verifyPayment_cancelledOrder_thenAutoCancelPayment")
    void verifyPayment_cancelledOrder_thenAutoCancelPayment() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntity.getOrderId()).thenReturn(1L);
        when(orderStatePort.getOrderState(1L)).thenReturn(OrderStatus.CANCELLED);

        PortOnePayment payment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "PAID",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        PortOnePayment cancelledPayment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "CANCELLED",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(paymentProviderService.getPayment("order-1")).thenReturn(payment, cancelledPayment);
        when(paymentProviderService.cancelPayment("order-1"))
                .thenReturn(new PortOneCancellation("cancel_1", "SUCCEEDED"));

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        paymentService.verifyPayment(1L, 1L, requestDto);

        verify(paymentProviderService).cancelPayment("order-1");
        verify(paymentVerificationTxService)
                .settleCancelledOrderPayment(1L, 1L, "imp_1", "pg_tid", 1000L);
    }

    @Test
    @DisplayName("verifyPayment_cancelledOrder_autoCancelFailed")
    void verifyPayment_cancelledOrder_autoCancelFailed() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
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
        when(paymentProviderService.cancelPayment("order-1"))
                .thenReturn(new PortOneCancellation("cancel_1", "FAILED"));

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.verifyPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.PAYMENT_REFUND_FAIL);

        verify(paymentVerificationTxService, never())
                .settleCancelledOrderPayment(anyLong(), anyLong(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("verifyPayment_cancelledOrder_alreadyCancelled_noOp")
    void verifyPayment_cancelledOrder_alreadyCancelled_noOp() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.CANCELLED);
        when(paymentEntity.getImpUid()).thenReturn("imp_1");
        when(paymentEntity.getPaidAmount()).thenReturn(1000L);
        when(paymentEntity.getOrderId()).thenReturn(1L);
        when(orderStatePort.getOrderState(1L)).thenReturn(OrderStatus.CANCELLED);

        PortOnePayment cancelledPayment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "CANCELLED",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(paymentProviderService.getPayment("order-1")).thenReturn(cancelledPayment);

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        paymentService.verifyPayment(1L, 1L, requestDto);

        verify(paymentProviderService, never()).cancelPayment("order-1");
        verify(paymentVerificationTxService, never())
                .syncCancelledOrderPayment(anyLong(), anyLong(), anyString(), anyString(), anyLong());
        verify(paymentVerificationTxService, never())
                .settleCancelledOrderPayment(anyLong(), anyLong(), anyString(), anyString(), anyLong());
        verify(paymentVerificationTxService, never())
                .confirmPaidPayment(anyLong(), anyLong(), anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("verifyPayment_cancelledOrder_pgCancelled_thenSyncLocalState")
    void verifyPayment_cancelledOrder_pgCancelled_thenSyncLocalState() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntity.getOrderId()).thenReturn(1L);
        when(orderStatePort.getOrderState(1L)).thenReturn(OrderStatus.CANCELLED);

        PortOnePayment cancelledPayment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "CANCELLED",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(paymentProviderService.getPayment("order-1")).thenReturn(cancelledPayment);

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        paymentService.verifyPayment(1L, 1L, requestDto);

        verify(paymentVerificationTxService)
                .syncCancelledOrderPayment(1L, 1L, "imp_1", "pg_tid", 1000L);
        verify(paymentProviderService, never()).cancelPayment("order-1");
    }

    @Test
    @DisplayName("verifyPayment_alreadyPaid_shouldNotDowngradeOrderStatus")
    void verifyPayment_alreadyPaid_shouldNotDowngradeOrderStatus() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
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

        verify(paymentVerificationTxService)
                .confirmPaidPayment(1L, 1L, "imp_1", "pg_tid", 1000L);
    }

    @Test
    @DisplayName("verifyPayment_alreadyPaid_pendingOrder_thenSetPaid")
    void verifyPayment_alreadyPaid_pendingOrder_thenSetPaid() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
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

        verify(paymentVerificationTxService)
                .confirmPaidPayment(1L, 1L, "imp_1", "pg_tid", 1000L);
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
    @DisplayName("refundPayment_pending_notCancelled_thenRetryCancel")
    void refundPayment_pending_notCancelled_thenRetryCancel() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.REFUND_PENDING);
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));

        PortOnePayment pendingPayment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "PAID",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        PortOnePayment cancelledPayment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "CANCELLED",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(paymentProviderService.getPayment("order-1"))
                .thenReturn(pendingPayment)
                .thenReturn(cancelledPayment);
        when(paymentProviderService.cancelPayment("order-1"))
                .thenReturn(new PortOneCancellation("cancel_1", "SUCCEEDED"));

        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(1000L);

        paymentService.refundPayment(1L, 1L, requestDto);

        verify(paymentProviderService).cancelPayment("order-1");
        verify(paymentRefundTxService).confirmRefund(1L, 1L);
        verify(paymentRefundTxService, never()).rollbackRefundPending(1L, 1L);
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

        verify(orderRefundPort).validateRefundApproved(1L);
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

        verify(orderRefundPort).validateRefundApproved(1L);
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

        verify(orderRefundPort).validateRefundApproved(1L);
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

        verify(orderRefundPort).validateRefundApproved(1L);
        verify(paymentRefundTxService).markRefundPending(1L, 1L);
        verify(paymentRefundTxService).confirmRefund(1L, 1L);
    }

    @Test
    @DisplayName("refundPayment_refundRequestNotApproved")
    void refundPayment_refundRequestNotApproved() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getPaidAmount()).thenReturn(1000L);
        when(paymentEntity.getOrderId()).thenReturn(1L);
        when(paymentEntityRepository.findByIdAndMemberId(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(orderStatePort.getOrderState(1L)).thenReturn(OrderStatus.PAID);
        doThrow(new BusinessException(BusinessErrorCode.ORDER_REFUND_REQUEST_NOT_APPROVED))
                .when(orderRefundPort).validateRefundApproved(1L);

        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.refundPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.ORDER_REFUND_REQUEST_NOT_APPROVED);

        verify(paymentRefundTxService, never()).markRefundPending(anyLong(), anyLong());
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

    private PaymentEntity mockDuplicatedPayment(Long memberId, Long paymentId) {
        PaymentEntity duplicatedPayment = mock(PaymentEntity.class);
        when(duplicatedPayment.getMemberId()).thenReturn(memberId);
        when(duplicatedPayment.getId()).thenReturn(paymentId);
        when(duplicatedPayment.getMerchantUid()).thenReturn("order-1");
        when(duplicatedPayment.getPrice()).thenReturn(1000L);
        when(duplicatedPayment.getPgProvider()).thenReturn("kakaopay");
        when(duplicatedPayment.getPayMethod()).thenReturn("card");
        when(duplicatedPayment.getStatus()).thenReturn(PaymentStatus.READY);
        return duplicatedPayment;
    }

}




