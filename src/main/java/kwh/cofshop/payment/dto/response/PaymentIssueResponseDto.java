package kwh.cofshop.payment.dto.response;

import java.util.List;

public record PaymentIssueResponseDto(
        List<Long> stuckApprovedRefundOrderIds,
        List<PaymentSummary> refundPendingPayments,
        List<PaymentOperationLogResponseDto> mismatches,
        long recoveryQueueSize,
        long recoveryDeadLetterSize
) {

    public record PaymentSummary(Long paymentId, Long orderId, String status, Long price, Long paidAmount) {
    }
}
