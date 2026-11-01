package kwh.cofshop.payment.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.order.api.OrderRefundPort;
import kwh.cofshop.order.api.OrderStatePort;
import kwh.cofshop.payment.client.portone.PortOnePayment;
import kwh.cofshop.payment.domain.PaymentEntity;
import kwh.cofshop.payment.domain.PaymentOperationLog;
import kwh.cofshop.payment.domain.PaymentStatus;
import kwh.cofshop.payment.dto.response.PaymentComparisonResponseDto;
import kwh.cofshop.payment.dto.response.PaymentIssueResponseDto;
import kwh.cofshop.payment.dto.response.PaymentOperationLogResponseDto;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import kwh.cofshop.payment.repository.PaymentOperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentAdminService {

    private static final long APPROVED_STUCK_MINUTES = 30L;
    private static final int ISSUE_LIMIT = 50;

    private final PaymentEntityRepository paymentEntityRepository;
    private final PaymentOperationLogRepository logRepository;
    private final PaymentOperationLogService logService;
    private final PaymentProviderService paymentProviderService;
    private final PaymentService paymentService;
    private final ApprovedRefundExecutor approvedRefundExecutor;
    private final RefundCompensationRecoveryQueueService recoveryQueueService;
    private final OrderRefundPort orderRefundPort;
    private final OrderStatePort orderStatePort;

    public PaymentIssueResponseDto getIssues() {
        List<Long> stuckOrderIds = orderRefundPort.findPendingApprovedRefundOrderIds(
                LocalDateTime.now().minusMinutes(APPROVED_STUCK_MINUTES), ISSUE_LIMIT);
        List<PaymentIssueResponseDto.PaymentSummary> refundPending = paymentEntityRepository
                .findTop50ByStatusOrderByIdAsc(PaymentStatus.REFUND_PENDING)
                .stream()
                .map(payment -> new PaymentIssueResponseDto.PaymentSummary(
                        payment.getId(), payment.getOrderId(), payment.getStatus().name(),
                        payment.getPrice(), payment.getPaidAmount()))
                .toList();
        List<PaymentOperationLogResponseDto> mismatches = logRepository
                .findTop50ByActionOrderByIdDesc(PaymentOperationLog.Action.MISMATCH_DETECTED)
                .stream()
                .map(PaymentOperationLogResponseDto::from)
                .toList();

        return new PaymentIssueResponseDto(
                stuckOrderIds,
                refundPending,
                mismatches,
                recoveryQueueService.pendingSize(),
                recoveryQueueService.deadLetterSize()
        );
    }

    public PaymentComparisonResponseDto compare(Long paymentId) {
        PaymentEntity payment = findPayment(paymentId);
        // PG 장애 중에도 DB 상태와 처리 이력은 볼 수 있어야 함
        PortOnePayment provider = null;
        boolean providerAvailable = true;
        try {
            provider = paymentProviderService.findPayment(payment.getMerchantUid()).orElse(null);
        } catch (BusinessException e) {
            if (e.getErrorCode() != BusinessErrorCode.PAYMENT_PROVIDER_ERROR) {
                throw e;
            }
            providerAvailable = false;
        }

        return new PaymentComparisonResponseDto(
                payment.getId(),
                payment.getOrderId(),
                orderStatePort.getOrderState(payment.getOrderId()).name(),
                payment.getStatus().name(),
                payment.getPrice(),
                payment.getPaidAmount(),
                payment.getImpUid(),
                providerAvailable,
                provider != null,
                provider == null ? null : provider.status(),
                provider == null ? null : provider.paidAmount(),
                provider == null ? null : provider.transactionId(),
                providerAvailable && isConsistent(payment, provider),
                logRepository.findTop50ByPaymentIdOrderByIdDesc(paymentId).stream()
                        .map(PaymentOperationLogResponseDto::from)
                        .toList()
        );
    }

    public void reconcile(Long adminId, Long paymentId) {
        PaymentEntity payment = findPayment(paymentId);
        runAndRecord(adminId, payment, PaymentOperationLog.Action.RECONCILE,
                () -> paymentService.reconcilePayment(paymentId));
    }

    public void retryRefund(Long adminId, Long paymentId) {
        PaymentEntity payment = findPayment(paymentId);
        runAndRecord(adminId, payment, PaymentOperationLog.Action.REFUND_RETRY,
                () -> approvedRefundExecutor.execute(payment.getOrderId()));
    }

    private void runAndRecord(Long adminId, PaymentEntity payment, PaymentOperationLog.Action action, Runnable work) {
        try {
            work.run();
        } catch (BusinessException e) {
            logService.record(payment.getId(), payment.getOrderId(), adminId, action,
                    PaymentOperationLog.Result.FAILED, e.getErrorCode().getCode());
            throw e;
        } catch (RuntimeException e) {
            logService.record(payment.getId(), payment.getOrderId(), adminId, action,
                    PaymentOperationLog.Result.FAILED, "INTERNAL");
            throw e;
        }
        logService.record(payment.getId(), payment.getOrderId(), adminId, action,
                PaymentOperationLog.Result.SUCCESS, null);
    }

    private boolean isConsistent(PaymentEntity payment, PortOnePayment provider) {
        return switch (payment.getStatus()) {
            case PAID -> provider != null
                    && "PAID".equalsIgnoreCase(provider.status())
                    && Objects.equals(payment.getPaidAmount(), provider.paidAmount())
                    && Objects.equals(payment.getImpUid(), provider.transactionId());
            case CANCELLED -> provider != null && "CANCELLED".equalsIgnoreCase(provider.status());
            // 아직 돈이 움직이지 않아야 하는 상태. PG에 없거나 미결제면 일치
            case READY, FAILED -> provider == null
                    || (!"PAID".equalsIgnoreCase(provider.status()) && !"CANCELLED".equalsIgnoreCase(provider.status()));
            // 환불 진행 중은 확정 전이므로 항상 확인 대상
            case REFUND_PENDING -> false;
        };
    }

    private PaymentEntity findPayment(Long paymentId) {
        return paymentEntityRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND));
    }
}
