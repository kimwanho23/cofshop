package kwh.cofshop.payment.service;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.repository.OrderRepository;
import kwh.cofshop.payment.domain.PaymentEntity;
import kwh.cofshop.payment.domain.PaymentStatus;
import kwh.cofshop.payment.dto.PaymentPrepareRequestDto;
import kwh.cofshop.payment.dto.PaymentRefundRequestDto;
import kwh.cofshop.payment.dto.PaymentResponseDto;
import kwh.cofshop.payment.dto.PaymentVerifyRequestDto;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentEntityRepository paymentEntityRepository;

    @Mock
    private IamportClient iamportClient;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("결제 정보 조회: 잘못된 UID")
    void getPaymentByImpUid_invalid() {
        assertThatThrownBy(() -> paymentService.getPaymentByImpUid(" "))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("결제 정보 조회: Iamport 오류")
    void getPaymentByImpUid_error() throws Exception {
        when(iamportClient.paymentByImpUid("imp_1")).thenThrow(new IOException("error"));

        assertThatThrownBy(() -> paymentService.getPaymentByImpUid("imp_1"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("결제 정보 조회: 성공")
    void getPaymentByImpUid_success() throws Exception {
        Payment payment = mock(Payment.class);
        IamportResponse<Payment> response = mock(IamportResponse.class);
        when(response.getResponse()).thenReturn(payment);
        when(iamportClient.paymentByImpUid("imp_1")).thenReturn(response);

        Payment result = paymentService.getPaymentByImpUid("imp_1");

        assertThat(result).isSameAs(payment);
    }

    @Test
    @DisplayName("결제 요청 생성: 주문 없음")
    void createPaymentRequest_notFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        PaymentPrepareRequestDto requestDto = PaymentPrepareRequestDto.builder()
                .pgProvider("kakaopay")
                .payMethod("card")
                .build();

        assertThatThrownBy(() -> paymentService.createPaymentRequest(1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("결제 요청 생성: 성공")
    void createPaymentRequest_success() {
        Order order = createOrder();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentEntityRepository.save(any(PaymentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentPrepareRequestDto requestDto = PaymentPrepareRequestDto.builder()
                .pgProvider("kakaopay")
                .payMethod("card")
                .build();

        PaymentResponseDto response = paymentService.createPaymentRequest(1L, requestDto);

        assertThat(response.getPgProvider()).isEqualTo("kakaopay");
        assertThat(response.getPayMethod()).isEqualTo("card");
        assertThat(order.getOrderState()).isEqualTo(OrderState.PAYMENT_PENDING);
    }

    @Test
    @DisplayName("결제 검증: 결제 정보 없음")
    void verifyPayment_notFound() {
        when(paymentEntityRepository.findById(1L)).thenReturn(Optional.empty());

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.verifyPayment(1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("결제 검증: Iamport 오류")
    void verifyPayment_iamportError() throws Exception {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findById(1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntity.getPrice()).thenReturn(1000L);
        when(iamportClient.paymentByImpUid("imp_1")).thenThrow(new IOException("error"));

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.verifyPayment(1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("결제 검증: 주문 번호 불일치")
    void verifyPayment_uidMismatch() throws Exception {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findById(1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");

        Payment mockIamportPayment = mock(Payment.class);
        when(mockIamportPayment.getMerchantUid()).thenReturn("order-2");

        IamportResponse<Payment> response = mock(IamportResponse.class);
        when(response.getResponse()).thenReturn(mockIamportPayment);
        when(iamportClient.paymentByImpUid("imp_1")).thenReturn(response);

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.verifyPayment(1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("결제 검증: 금액 불일치")
    void verifyPayment_amountMismatch() throws Exception {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findById(1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntity.getPrice()).thenReturn(1000L);

        Payment mockIamportPayment = mock(Payment.class);
        when(mockIamportPayment.getMerchantUid()).thenReturn("order-1");
        when(mockIamportPayment.getAmount()).thenReturn(BigDecimal.valueOf(900L));

        IamportResponse<Payment> response = mock(IamportResponse.class);
        when(response.getResponse()).thenReturn(mockIamportPayment);
        when(iamportClient.paymentByImpUid("imp_1")).thenReturn(response);

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.verifyPayment(1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("결제 검증: 성공")
    void verifyPayment_success() throws Exception {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findById(1L)).thenReturn(Optional.of(paymentEntity));
        when(paymentEntity.getMerchantUid()).thenReturn("order-1");
        when(paymentEntity.getPrice()).thenReturn(1000L);

        Payment mockIamportPayment = mock(Payment.class);
        when(mockIamportPayment.getImpUid()).thenReturn("imp_1");
        when(mockIamportPayment.getMerchantUid()).thenReturn("order-1");
        when(mockIamportPayment.getPgTid()).thenReturn("pg_tid");
        when(mockIamportPayment.getAmount()).thenReturn(BigDecimal.valueOf(1000L));

        IamportResponse<Payment> response = mock(IamportResponse.class);
        when(response.getResponse()).thenReturn(mockIamportPayment);
        when(iamportClient.paymentByImpUid("imp_1")).thenReturn(response);

        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order-1");
        requestDto.setAmount(1000L);

        paymentService.verifyPayment(1L, requestDto);

        verify(paymentEntity).paymentSuccess(eq("imp_1"), eq("pg_tid"), eq(1000L), any(LocalDateTime.class));
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
    @DisplayName("결제 환불: Iamport 오류")
    void refundPayment_iamportError() throws Exception {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getImpUid()).thenReturn("imp_1");
        when(paymentEntity.getPaidAmount()).thenReturn(1000L);
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));
        when(iamportClient.cancelPaymentByImpUid(any(CancelData.class))).thenThrow(new IOException("error"));

        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.refundPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("결제 환불: 환불 실패")
    void refundPayment_failed() throws Exception {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getImpUid()).thenReturn("imp_1");
        when(paymentEntity.getPaidAmount()).thenReturn(1000L);
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));

        IamportResponse<Payment> response = mock(IamportResponse.class);
        when(response.getResponse()).thenReturn(null);
        when(iamportClient.cancelPaymentByImpUid(any(CancelData.class))).thenReturn(response);

        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(1000L);

        assertThatThrownBy(() -> paymentService.refundPayment(1L, 1L, requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("결제 환불: 성공")
    void refundPayment_success() throws Exception {
        PaymentEntity paymentEntity = mock(PaymentEntity.class);
        when(paymentEntity.getStatus()).thenReturn(PaymentStatus.PAID);
        when(paymentEntity.getImpUid()).thenReturn("imp_1");
        when(paymentEntity.getPaidAmount()).thenReturn(1000L);
        when(paymentEntityRepository.findByIdAndMember_Id(1L, 1L)).thenReturn(Optional.of(paymentEntity));

        Payment payment = mock(Payment.class);
        when(payment.getStatus()).thenReturn("cancelled");
        IamportResponse<Payment> response = mock(IamportResponse.class);
        when(response.getResponse()).thenReturn(payment);
        when(iamportClient.cancelPaymentByImpUid(any(CancelData.class))).thenReturn(response);

        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(1000L);

        paymentService.refundPayment(1L, 1L, requestDto);

        verify(paymentEntity).paymentStatusChange(PaymentStatus.CANCELLED);
    }

    private Order createOrder() {
        Member member = Member.builder()
                .id(1L)
                .email("user@example.com")
                .memberName("사용자")
                .memberPwd("pw")
                .tel("01012341234")
                .build();

        Order order = Order.builder()
                .member(member)
                .merchantUid("order-1")
                .orderState(OrderState.WAITING_FOR_PAY)
                .address(kwh.cofshop.order.domain.Address.builder().city("서울").street("강남").zipCode("12345").build())
                .totalPrice(1000L)
                .finalPrice(1000L)
                .build();
        ReflectionTestUtils.setField(order, "id", 1L);
        return order;
    }
}
