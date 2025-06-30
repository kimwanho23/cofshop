package kwh.cofshop.payment.controller;

import com.siot.IamportRestClient.response.Payment;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import kwh.cofshop.global.response.ApiResponse;
import kwh.cofshop.payment.dto.*;
import kwh.cofshop.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 결제 정보 조회
    @Operation(summary = "결제 정보 조회", description = "결제 정보를 조회합니다.")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/iamport/{impUid}")
    public ResponseEntity<ApiResponse<Payment>> getPaymentInfo(@PathVariable String impUid) {
        Payment payment = paymentService.getPaymentByImpUid(impUid);
        return ResponseEntity.ok(ApiResponse.OK(payment));
    }

    // 결제 검증
    @Operation(summary = "결제 검증", description = "주문 데이터와 포트원 결제 데이터의 일치를 검증합니다.")
    @PostMapping("/{paymentId}/verify")
    public ResponseEntity<Void> verifyPayment(@PathVariable Long paymentId,
                              @RequestBody @Valid PaymentVerifyRequestDto requestDto) {
        paymentService.verifyPayment(paymentId, requestDto);
        return ResponseEntity.ok().build();
    }

    // 결제 생성
    @Operation(summary = "결제 생성", description = "상품의 결제 데이터를 생성합니다.")
    @PostMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> requestPayment(@PathVariable Long orderId,
                                                                          @RequestBody @Valid PaymentPrepareRequestDto requestDto) {
        PaymentResponseDto paymentRequest = paymentService.createPaymentRequest(orderId, requestDto);
        return ResponseEntity.created(URI.create("api/payments" + orderId))
                .body(ApiResponse.Created(paymentRequest));
    }

    // 결제 환불
    @Operation(summary = "환불", description = "결제한 상품의 금액을 환불합니다.")
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<Void> refundPayment(@RequestParam Long userId,
                              @RequestBody @Valid PaymentRefundRequestDto requestDto) {
        paymentService.refundPayment(userId, requestDto);
        return ResponseEntity.ok().build();
    }
}