package kwh.cofshop.payment.dto.response;

import java.util.List;

public record PaymentComparisonResponseDto(
        Long paymentId,
        Long orderId,
        String orderStatus,
        String dbStatus,
        Long dbPrice,
        Long dbPaidAmount,
        String dbTransactionId,
        boolean providerAvailable,
        boolean existsInProvider,
        String providerStatus,
        Long providerPaidAmount,
        String providerTransactionId,
        boolean consistent,
        List<PaymentOperationLogResponseDto> history
) {
}
