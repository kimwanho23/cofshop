package kwh.cofshop.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import kwh.cofshop.global.annotation.LoginMember;
import kwh.cofshop.payment.dto.response.PaymentComparisonResponseDto;
import kwh.cofshop.payment.dto.response.PaymentIssueResponseDto;
import kwh.cofshop.payment.service.PaymentAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class PaymentAdminController {

    private final PaymentAdminService paymentAdminService;

    @Operation(summary = "결제 문제 건 조회", description = "미완료 환불, 환불 진행 중 결제, PG 불일치, 복구 큐 크기를 조회합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/issues")
    public PaymentIssueResponseDto getIssues() {
        return paymentAdminService.getIssues();
    }

    @Operation(summary = "결제 DB·PG 비교", description = "DB와 PG의 결제 상태를 비교하고 처리 이력을 함께 조회합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{paymentId}/comparison")
    public PaymentComparisonResponseDto compare(@PathVariable Long paymentId) {
        return paymentAdminService.compare(paymentId);
    }

    @Operation(summary = "결제 수동 대사", description = "READY 결제를 PG 상태로 즉시 맞춥니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{paymentId}/reconcile")
    public ResponseEntity<Void> reconcile(
            @PathVariable Long paymentId,
            @Parameter(hidden = true) @LoginMember Long adminId) {
        paymentAdminService.reconcile(adminId, paymentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "환불 수동 재시도", description = "승인된 환불을 즉시 다시 실행합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{paymentId}/refund/retry")
    public ResponseEntity<Void> retryRefund(
            @PathVariable Long paymentId,
            @Parameter(hidden = true) @LoginMember Long adminId) {
        paymentAdminService.retryRefund(adminId, paymentId);
        return ResponseEntity.noContent().build();
    }
}
