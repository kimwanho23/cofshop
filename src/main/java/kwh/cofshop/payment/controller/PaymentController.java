package kwh.cofshop.payment.controller;

import com.siot.IamportRestClient.response.Payment;
import kwh.cofshop.global.response.ApiResponse;
import kwh.cofshop.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{impUid}")
    public ResponseEntity<ApiResponse<Payment>> getPaymentInfo(@PathVariable String impUid) {
        Payment payment = paymentService.getPaymentByImpUid(impUid);
        return ResponseEntity.ok(ApiResponse.OK(payment));
    }
}