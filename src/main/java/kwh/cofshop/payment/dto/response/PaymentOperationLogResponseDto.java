package kwh.cofshop.payment.dto.response;

import kwh.cofshop.payment.domain.PaymentOperationLog;

import java.time.LocalDateTime;

public record PaymentOperationLogResponseDto(
        Long id,
        Long paymentId,
        Long orderId,
        Long adminId,
        PaymentOperationLog.Action action,
        PaymentOperationLog.Result result,
        String errorCode,
        LocalDateTime createdAt
) {

    public static PaymentOperationLogResponseDto from(PaymentOperationLog log) {
        return new PaymentOperationLogResponseDto(
                log.getId(), log.getPaymentId(), log.getOrderId(), log.getAdminId(),
                log.getAction(), log.getResult(), log.getErrorCode(), log.getCreatedAt()
        );
    }
}
