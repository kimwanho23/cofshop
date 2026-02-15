package kwh.cofshop.payment.controller;

import kwh.cofshop.payment.dto.request.PaymentPrepareRequestDto;
import kwh.cofshop.payment.dto.response.PaymentProviderResponseDto;
import kwh.cofshop.payment.dto.response.PaymentResponseDto;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
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

        when(paymentService.createPaymentRequest(anyLong(), anyLong(), any())).thenReturn(responseDto);

        PaymentPrepareRequestDto requestDto = PaymentPrepareRequestDto.builder()
                .pgProvider("kakaopay")
                .payMethod("card")
                .build();

        mockMvc.perform(post("/api/payments/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "pgProvider":"%s",
                                  "payMethod":"%s"
                                }
                                """.formatted(requestDto.getPgProvider(), requestDto.getPayMethod())))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("결제 검증")
    void verifyPayment() throws Exception {
        mockMvc.perform(post("/api/payments/1/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "impUid":"imp_1",
                                  "merchantUid":"order_1",
                                  "amount":1000
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("결제 정보 조회")
    void getPaymentInfo() throws Exception {
        PaymentProviderResponseDto responseDto = PaymentProviderResponseDto.builder()
                .paymentId("order-1")
                .transactionId("imp_1")
                .build();

        when(paymentService.getPaymentByImpUid(anyLong(), org.mockito.ArgumentMatchers.eq("imp_1")))
                .thenReturn(responseDto);

        mockMvc.perform(get("/api/payments/iamport/imp_1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("결제 환불")
    void refundPayment() throws Exception {
        mockMvc.perform(post("/api/payments/1/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amount":1000
                                }
                                """))
                .andExpect(status().isNoContent());
    }
}
