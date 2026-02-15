package kwh.cofshop.payment.service;

import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.repository.OrderRepository;
import kwh.cofshop.payment.client.portone.PortOneCancellation;
import kwh.cofshop.payment.client.portone.PortOneClientException;
import kwh.cofshop.payment.client.portone.PortOnePayment;
import kwh.cofshop.payment.client.portone.PortOnePaymentClient;
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
import org.springframework.test.util.ReflectionTestUtils;

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
    private OrderRepository orderRepository;

    @Mock
    private PaymentEntityRepository paymentEntityRepository;

    @Mock
    private PortOnePaymentClient portOnePaymentClient;

    @Mock
    private PaymentRefundTxService paymentRefundTxService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("결제 정보 조회: 잘못된 UID")
    void getPaymentByImpUid_invalid() {
        assertThatThrownBy(() -> paymentService.getPaymentByImpUid(1L, " "))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("결제 정보 조회: PortOne 오류")
    void getPaymentByImpUid_error() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByImpUidAndMember_Id("imp_1", 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(portOnePaymentClient.getPayment("order-1")).thenThrow(new PortOneClientException("error"));

        assertThatThrownBy(() -> paymentService.getPaymentByImpUid(1L, "imp_1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("결제 정보 조회: transactionId 불일치")
    void getPaymentByImpUid_uidMismatch() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByImpUidAndMember_Id("imp_1", 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");

        PortOnePayment payment = new PortOnePayment(
                "order-1",
                "imp_2",
                "pg_tid",
                "PAID",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(portOnePaymentClient.getPayment("order-1")).thenReturn(payment);

        assertThatThrownBy(() -> paymentService.getPaymentByImpUid(1L, "imp_1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("결제 정보 조회: 성공")
    void getPaymentByImpUid_success() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByImpUidAndMember_Id("imp_1", 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");

        PortOnePayment payment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "PAID",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(portOnePaymentClient.getPayment("order-1")).thenReturn(payment);

        PaymentProviderResponseDto result = paymentService.getPaymentByImpUid(1L, "imp_1");

        assertThat(result.getPaymentId()).isEqualTo("order-1");
        assertThat(result.getTransactionId()).isEqualTo("imp_1");
        assertThat(result.getPgTxId()).isEqualTo("pg_tid");
        assertThat(result.getPaidAmount()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("결제 요청 생성: 주문 없음")
    void createPaymentRequest_notFound() {
        when(orderRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.empty());

        PaymentPrepareRequestDto requestDto = PaymentPrepareRequestDto.builder()
                .pgProvider("kakaopay")
                .payMethod("card")
                .build();

        assertThatThrownBy(() -> paymentService.createPaymentRequest(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("결제 요청 생성: 성공")
    void createPaymentRequest_success() {
        Order order = createOrder();
        when(orderRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(order));
        when(paymentEntityRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentPrepareRequestDto requestDto = PaymentPrepareRequestDto.builder()
                .pgProvider("kakaopay")
                .payMethod("card")
                .build();

        PaymentResponseDto response = paymentService.createPaymentRequest(1L, 1L, requestDto);

        assertThat(response.getPgProvider()).isEqualTo("kakaopay");
        assertThat(response.getPayMethod()).isEqualTo("card");
        verify(order).pay();
    }

    @Test
    @DisplayName("결제 검증: 결제 정보 없음")
    void verifyPayment_notFound() {
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.empty());

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.verifyPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("결제 검증: PortOne 오류")
    void verifyPayment_portOneError() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(portOnePaymentClient.getPayment("order-1")).thenThrow(new PortOneClientException("error"));

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.verifyPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("결제 검증: 주문 번호 불일치")
    void verifyPayment_uidMismatch() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));

        PortOnePayment payment = new PortOnePayment(
                "order-2",
                "imp_1",
                "pg_tid",
                "PAID",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(portOnePaymentClient.getPayment("order-1")).thenReturn(payment);

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.verifyPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("결제 검증: 결제 완료 상태 아님")
    void verifyPayment_notPaidStatus() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");

        PortOnePayment payment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "READY",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(portOnePaymentClient.getPayment("order-1")).thenReturn(payment);

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.verifyPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("결제 검증: 금액 불일치")
    void verifyPayment_amountMismatch() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");

        PortOnePayment payment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "PAID",
                new PortOnePayment.Amount(1000L, 900L)
        );
        when(portOnePaymentClient.getPayment("order-1")).thenReturn(payment);

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.verifyPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("결제 검증: 성공")
    void verifyPayment_success() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntity.getPrice()).thenReturn(1000L);
        Order order = mock(Order.class);
        when(paymentEntity.getOrder()).thenReturn(order);
        when(order.getOrderState()).thenReturn(OrderState.PAYMENT_PENDING);

        PortOnePayment payment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "PAID",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(portOnePaymentClient.getPayment("order-1")).thenReturn(payment);

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        paymentService.verifyPayment(1L, 1L, requestDto);

        verify(paymentEntity).paymentSuccess(eq("imp_1"), eq("pg_tid"), eq(1000L), any(LocalDateTime.class));
        verify(order).changeOrderState(OrderState.PAID);
    }

    @Test
    @DisplayName("결제 검증: 취소된 주문은 결제 완료로 전이되지 않음")
    void verifyPayment_cancelledOrder_shouldFail() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        Order order = mock(Order.class);
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.READY);
        when(paymentEntity.getPrice()).thenReturn(1000L);
        when(paymentEntity.getOrder()).thenReturn(order);
        when(order.getOrderState()).thenReturn(OrderState.CANCELLED);

        PortOnePayment payment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "PAID",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(portOnePaymentClient.getPayment("order-1")).thenReturn(payment);

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.verifyPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.ORDER_ALREADY_CANCELLED);

        verify(paymentEntity, never()).paymentSuccess(anyString(), anyString(), anyLong(), any(LocalDateTime.class));
        verify(order, never()).changeOrderState(OrderState.PAID);
    }

    @Test
    @DisplayName("결제 검증: 이미 결제된 건에서 주문 상태는 역행시키지 않음")
    void verifyPayment_alreadyPaid_shouldNotDowngradeOrderState() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        Order order = mock(Order.class);
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getImpUid()).thenReturn("imp_1");
        when(paymentEntity.getPaidAmount()).thenReturn(1000L);
        when(paymentEntity.getOrder()).thenReturn(order);
        when(order.getOrderState()).thenReturn(OrderState.DELIVERED);

        PortOnePayment payment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "PAID",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(portOnePaymentClient.getPayment("order-1")).thenReturn(payment);

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        paymentService.verifyPayment(1L, 1L, requestDto);

        verify(paymentEntity, never()).paymentSuccess(anyString(), anyString(), anyLong(), any(LocalDateTime.class));
        verify(order, never()).changeOrderState(OrderState.PAID);
    }

    @Test
    @DisplayName("결제 검증: 이미 결제된 건이 결제요청 상태면 PAID로 보정")
    void verifyPayment_alreadyPaid_pendingOrder_thenSetPaid() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        Order order = mock(Order.class);
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getImpUid()).thenReturn("imp_1");
        when(paymentEntity.getPaidAmount()).thenReturn(1000L);
        when(paymentEntity.getOrder()).thenReturn(order);
        when(order.getOrderState()).thenReturn(OrderState.PAYMENT_PENDING);

        PortOnePayment payment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "PAID",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(portOnePaymentClient.getPayment("order-1")).thenReturn(payment);

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        paymentService.verifyPayment(1L, 1L, requestDto);

        verify(order).changeOrderState(OrderState.PAID);
    }

    @Test
    @DisplayName("결제 환불: 이미 취소")
    void refundPayment_alreadyCancelled() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.CANCELLED);
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));

        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.refundPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("결제 환불: 환불 불가 상태")
    void refundPayment_notPaid() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.READY);
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));

        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.refundPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("결제 환불: REFUND_PENDING 상태 재시도 시 확정 처리")
    void refundPayment_pending_thenConfirm() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.REFUND_PENDING);
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));

        PortOnePayment payment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "CANCELLED",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(portOnePaymentClient.getPayment("order-1")).thenReturn(payment);

        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(1000L);

        paymentService.refundPayment(1L, 1L, requestDto);

        verify(paymentRefundTxService).confirmRefund(1L, 1L);
    }

    @Test
    @DisplayName("결제 환불: PortOne 오류")
    void refundPayment_portOneError() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getPaidAmount()).thenReturn(1000L);
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntityRepository.existsByIdAndMember_IdAndOrder_OrderState(1L, 1L, OrderState.PAID)).thenReturn(true);
        when(portOnePaymentClient.cancelPayment(eq("order-1"), anyString())).thenThrow(new PortOneClientException("error"));
        when(portOnePaymentClient.getPayment("order-1")).thenThrow(new PortOneClientException("error"));

        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.refundPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);

        verify(paymentRefundTxService).markRefundPending(1L, 1L);
        verify(paymentRefundTxService).rollbackRefundPending(1L, 1L);
    }

    @Test
    @DisplayName("결제 환불: 환불 실패")
    void refundPayment_failed() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getPaidAmount()).thenReturn(1000L);
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntityRepository.existsByIdAndMember_IdAndOrder_OrderState(1L, 1L, OrderState.PAID)).thenReturn(true);

        PortOneCancellation cancellation = new PortOneCancellation("cancel_1", "FAILED");
        when(portOnePaymentClient.cancelPayment(eq("order-1"), anyString())).thenReturn(cancellation);

        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.refundPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);

        verify(paymentRefundTxService).markRefundPending(1L, 1L);
        verify(paymentRefundTxService).rollbackRefundPending(1L, 1L);
    }

    @Test
    @DisplayName("결제 환불: cancel 호출 후 상태 불일치")
    void refundPayment_statusNotCancelled() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getPaidAmount()).thenReturn(1000L);
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntityRepository.existsByIdAndMember_IdAndOrder_OrderState(1L, 1L, OrderState.PAID)).thenReturn(true);

        PortOneCancellation cancellation = new PortOneCancellation("cancel_1", "SUCCEEDED");
        when(portOnePaymentClient.cancelPayment(eq("order-1"), anyString())).thenReturn(cancellation);
        PortOnePayment payment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "PAID",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(portOnePaymentClient.getPayment("order-1")).thenReturn(payment);

        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.refundPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);

        verify(paymentRefundTxService).markRefundPending(1L, 1L);
        verify(paymentRefundTxService).rollbackRefundPending(1L, 1L);
    }

    @Test
    @DisplayName("결제 환불: 성공")
    void refundPayment_success() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getPaidAmount()).thenReturn(1000L);
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntityRepository.existsByIdAndMember_IdAndOrder_OrderState(1L, 1L, OrderState.PAID)).thenReturn(true);

        PortOneCancellation cancellation = new PortOneCancellation("cancel_1", "SUCCEEDED");
        when(portOnePaymentClient.cancelPayment(eq("order-1"), anyString())).thenReturn(cancellation);
        PortOnePayment payment = new PortOnePayment(
                "order-1",
                "imp_1",
                "pg_tid",
                "CANCELLED",
                new PortOnePayment.Amount(1000L, 1000L)
        );
        when(portOnePaymentClient.getPayment("order-1")).thenReturn(payment);

        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(1000L);

        paymentService.refundPayment(1L, 1L, requestDto);

        verify(paymentRefundTxService).markRefundPending(1L, 1L);
        verify(paymentRefundTxService).confirmRefund(1L, 1L);
    }

    @Test
    @DisplayName("결제 환불: 주문 상태가 결제완료가 아님")
    void refundPayment_orderStateNotPaid() {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getPaidAmount()).thenReturn(1000L);
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntityRepository.existsByIdAndMember_IdAndOrder_OrderState(1L, 1L, OrderState.PAID)).thenReturn(false);

        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.refundPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    private Order createOrder() {
        Member member = mock(Member.class);
        ReflectionTestUtils.setField(member, "id", 1L);
        ReflectionTestUtils.setField(member, "email", "test@test.com");
        ReflectionTestUtils.setField(member, "memberName", "tester");
        ReflectionTestUtils.setField(member, "tel", "010-0000-0000");

        Order order = mock(Order.class);
        ReflectionTestUtils.setField(order, "id", 1L);
        when(order.getMember()).thenReturn(member);
        when(order.getFinalPrice()).thenReturn(1000L);

        return order;
    }

}
