package kwh.cofshop.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "payment_operation_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentOperationLog {

    public enum Action {
        RECONCILE, REFUND_RETRY, MISMATCH_DETECTED
    }

    public enum Result {
        SUCCESS, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_operation_log_id")
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "admin_id")
    private Long adminId; // 시스템이 남긴 이력은 null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Action action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Result result;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static PaymentOperationLog of(Long paymentId, Long orderId, Long adminId, Action action, Result result,
                                         String errorCode) {
        PaymentOperationLog log = new PaymentOperationLog();
        log.paymentId = paymentId;
        log.orderId = orderId;
        log.adminId = adminId;
        log.action = action;
        log.result = result;
        log.errorCode = errorCode;
        log.createdAt = LocalDateTime.now();
        return log;
    }
}
