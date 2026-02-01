package kwh.cofshop.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siot.IamportRestClient.response.Payment;
import kwh.cofshop.payment.dto.PaymentPrepareRequestDto;
import kwh.cofshop.payment.dto.PaymentRefundRequestDto;
import kwh.cofshop.payment.dto.PaymentResponseDto;
import kwh.cofshop.payment.dto.PaymentVerifyRequestDto;
import kwh.cofshop.payment.service.PaymentService;
import kwh.cofshop.support.StandaloneMockMvcFactory;
import kwh.cofshop.support.TestLoginMemberArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = StandaloneMockMvcFactory.build(
                paymentController,
                new TestLoginMemberArgumentResolver()
        );
    }

    @Test
    @DisplayName("결제 요청 생성")
    void createPayment() throws Exception {
        PaymentResponseDto responseDto = PaymentResponseDto.builder()
                .paymentId(1L)
                .build();

        when(paymentService.createPaymentRequest(anyLong(), any())).thenReturn(responseDto);

        PaymentPrepareRequestDto requestDto = PaymentPrepareRequestDto.builder()
                .pgProvider("kakaopay")
                .payMethod("card")
                .build();

        mockMvc.perform(post("/api/payments/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("결제 검증")
    void verifyPayment() throws Exception {
        PaymentVerifyRequestDto requestDto = new PaymentVerifyRequestDto();
        requestDto.setImpUid("imp_1");
        requestDto.setMerchantUid("order_1");
        requestDto.setAmount(1000L);

        mockMvc.perform(post("/api/payments/1/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("결제 정보 조회")
    void getPaymentInfo() throws Exception {
        Payment payment = mock(Payment.class);
        when(paymentService.getPaymentByImpUid("imp_1")).thenReturn(payment);

        mockMvc.perform(get("/api/payments/iamport/imp_1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("결제 환불")
    void refundPayment() throws Exception {
        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(1000L);

        mockMvc.perform(post("/api/payments/1/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNoContent());
    }
}
