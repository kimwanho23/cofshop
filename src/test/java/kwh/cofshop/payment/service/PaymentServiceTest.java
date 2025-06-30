package kwh.cofshop.payment.service;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.order.domain.Address;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.OrderItem;
import kwh.cofshop.order.repository.OrderRepository;
import kwh.cofshop.payment.domain.PaymentEntity;
import kwh.cofshop.payment.dto.PaymentPrepareRequestDto;
import kwh.cofshop.payment.dto.PaymentResponseDto;
import kwh.cofshop.payment.dto.PaymentVerifyRequestDto;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentEntityRepository paymentEntityRepository;

    @Mock
    private IamportClient iamportClient;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        PaymentService paymentService = mock(PaymentService.class);
    }

    @Test
    @DisplayName("결제 단위 테스트")
    void payment_Test() throws InterruptedException {
        Long orderId = 1L;

        Member mockMember = mock(Member.class);
        when(mockMember.getEmail()).thenReturn("test@cofshop.com");
        when(mockMember.getMemberName()).thenReturn("테스트유저");
        when(mockMember.getTel()).thenReturn("010-1234-5678");

        Order mockOrder = mock(Order.class);
        when(mockOrder.getMember()).thenReturn(mockMember);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(mockOrder));

        PaymentPrepareRequestDto requestDto = PaymentPrepareRequestDto.builder()
                .pgProvider("kakaopay")
                .payMethod("card")
                .build();

        PaymentResponseDto response = paymentService.createPaymentRequest(orderId, requestDto);


        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPgProvider()).isEqualTo("kakaopay");
        assertThat(response.getPayMethod()).isEqualTo("card");

        verify(orderRepository).findById(orderId);
        verify(mockOrder).pay();
        verify(paymentEntityRepository).save(any(PaymentEntity.class));
    }

    @Test
    @DisplayName("결제 검증 테스트")
    void verifyPayment_success() throws Exception {
        Long paymentId = 1L;

        // 검증 엔티티
        PaymentVerifyRequestDto verifyRequestDto = new PaymentVerifyRequestDto();
        verifyRequestDto.setImpUid("imp_123");
        verifyRequestDto.setMerchantUid("order-1");
        verifyRequestDto.setAmount(10000L);

        // payment 엔티티
        PaymentEntity mockPaymentEntity = mock(PaymentEntity.class);
        when(paymentEntityRepository.findById(paymentId)).thenReturn(Optional.of(mockPaymentEntity));

        // Mock Payment (아임포트 응답 객체)
        Payment mockIamportPayment = mock(Payment.class);
        when(mockIamportPayment.getImpUid()).thenReturn("imp_123");
        when(mockIamportPayment.getMerchantUid()).thenReturn("order-1");
        when(mockIamportPayment.getPgTid()).thenReturn("pg_tid_001");
        when(mockIamportPayment.getAmount()).thenReturn(BigDecimal.valueOf(10000L));

        // Mock IamportResponse
        IamportResponse<Payment> mockResponse = mock(IamportResponse.class);
        when(mockResponse.getResponse()).thenReturn(mockIamportPayment);

        // IamportClient mock 설정
        when(iamportClient.paymentByImpUid("imp_123")).thenReturn(mockResponse);

        // when
        paymentService.verifyPayment(paymentId, verifyRequestDto);

        // then
        verify(mockPaymentEntity).paymentSuccess(
                eq("imp_123"),
                eq("pg_tid_001"),
                eq(10000L),
                any(LocalDateTime.class)
        );
    }

}