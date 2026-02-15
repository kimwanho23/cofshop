package kwh.cofshop.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import kwh.cofshop.argumentResolver.LoginMember;
import kwh.cofshop.payment.dto.request.PaymentPrepareRequestDto;
import kwh.cofshop.payment.dto.response.PaymentProviderResponseDto;
import kwh.cofshop.payment.dto.request.PaymentRefundRequestDto;
import kwh.cofshop.payment.dto.response.PaymentResponseDto;
import kwh.cofshop.payment.dto.request.PaymentVerifyRequestDto;
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
    @PreAuthorize("hasRole('MEMBER')")
    @GetMapping("/iamport/{impUid}")
    public PaymentProviderResponseDto getPaymentInfo(@LoginMember Long memberId, @PathVariable String impUid) {
        return paymentService.getPaymentByImpUid(memberId, impUid);
    }

    @Operation(summary = "Verify payment")
    @PreAuthorize("hasRole('MEMBER')")
    @PostMapping("/{paymentId}/verify")
    public ResponseEntity<Void> verifyPayment(@PathVariable Long paymentId,
                                              @LoginMember Long memberId,
                                              @RequestBody @Valid PaymentVerifyRequestDto requestDto) {
        paymentService.verifyPayment(memberId, paymentId, requestDto);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Create payment request")
    @PreAuthorize("hasRole('MEMBER')")
    @PostMapping("/orders/{orderId}")
    public ResponseEntity<PaymentResponseDto> requestPayment(@PathVariable Long orderId,
                                                             @LoginMember Long memberId,
                                                             @RequestBody @Valid PaymentPrepareRequestDto requestDto) {
        PaymentResponseDto paymentRequest = paymentService.createPaymentRequest(memberId, orderId, requestDto);
        return ResponseEntity.created(URI.create("/api/payments/" + paymentRequest.getPaymentId()))
                .body(paymentRequest);
    }

    @Operation(summary = "Refund payment")
    @PreAuthorize("hasRole('MEMBER')")
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<Void> refundPayment(@PathVariable Long paymentId,
                                              @LoginMember Long memberId,
                                              @RequestBody @Valid PaymentRefundRequestDto requestDto) {
        paymentService.refundPayment(paymentId, memberId, requestDto);
        return ResponseEntity.noContent().build();
    }
}
