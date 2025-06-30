package kwh.cofshop.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.payment.dto.*;
import kwh.cofshop.payment.service.PaymentService;
import kwh.cofshop.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class) // Spring Security 커스터마이징 시 필요
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @InjectMocks
    private PaymentService paymentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PaymentVerifyRequestDto verifyRequestDto;
    private PaymentRefundRequestDto refundRequestDto;
    private PaymentPrepareRequestDto prepareRequestDto;
    private PaymentResponseDto responseDto;

    @BeforeEach
    void setUp() {
    }

    @Test
    void requestPayment_success() throws Exception {
        given(paymentService.createPaymentRequest(eq(1L), any(PaymentPrepareRequestDto.class)))
                .willReturn(responseDto);

        mockMvc.perform(post("/api/payments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prepareRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.paymentId").value(1L))
                .andExpect(jsonPath("$.result.status").value("PAID"));
    }


    @Test
    void verifyPayment_success() throws Exception {
        mockMvc.perform(post("/api/payments/verify/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequestDto)))
                .andExpect(status().isOk());

        verify(paymentService).verifyPayment(eq(1L), any(PaymentVerifyRequestDto.class));
    }
}