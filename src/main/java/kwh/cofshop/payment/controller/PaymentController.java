package kwh.cofshop.payment.controller;

import com.siot.IamportRestClient.response.Payment;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import kwh.cofshop.argumentResolver.LoginMember;
import kwh.cofshop.payment.dto.PaymentPrepareRequestDto;
import kwh.cofshop.payment.dto.PaymentRefundRequestDto;
import kwh.cofshop.payment.dto.PaymentResponseDto;
import kwh.cofshop.payment.dto.PaymentVerifyRequestDto;
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

    @Operation(summary = "Get payment info")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/iamport/{impUid}")
    public Payment getPaymentInfo(@PathVariable String impUid) {
        Payment payment = paymentService.getPaymentByImpUid(impUid);
        return payment;
    }

    @Operation(summary = "Verify payment")
    @PostMapping("/{paymentId}/verify")
    public ResponseEntity<Void> verifyPayment(@PathVariable Long paymentId,
                                              @RequestBody @Valid PaymentVerifyRequestDto requestDto) {
        paymentService.verifyPayment(paymentId, requestDto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Create payment request")
    @PostMapping("/orders/{orderId}")
    public ResponseEntity<PaymentResponseDto> requestPayment(@PathVariable Long orderId,
                                                                          @RequestBody @Valid PaymentPrepareRequestDto requestDto) {
        PaymentResponseDto paymentRequest = paymentService.createPaymentRequest(orderId, requestDto);
        return ResponseEntity.created(URI.create("api/payments" + orderId))
                .body(paymentRequest);
    }

    @Operation(summary = "Refund payment")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<Void> refundPayment(@PathVariable Long paymentId,
                                              @LoginMember Long memberId,
                                              @RequestBody @Valid PaymentRefundRequestDto requestDto) {
        paymentService.refundPayment(paymentId, memberId, requestDto);
        return ResponseEntity.noContent().build();
    }
}
