package kwh.cofshop.payment.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.order.api.OrderRefundPort;
import kwh.cofshop.order.api.OrderStatus;
import kwh.cofshop.order.api.OrderStatePort;
import kwh.cofshop.payment.domain.PaymentEntity;
import kwh.cofshop.payment.domain.PaymentStatus;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentRefundTxService {

    private static final String REFUND_COMPLETED_REASON = "결제 환불 완료";

    private final PaymentEntityRepository paymentEntityRepository;
    private final OrderRefundPort orderRefundPort;
    private final OrderStatePort orderStatePort;
    private final RefundCompensationRecoveryQueueService recoveryQueueService;

    @Transactional
    public void markRefundPending(Long paymentId, Long memberId) {
        PaymentEntity paymentEntity = paymentEntityRepository
                .findByIdAndMemberId(paymentId, memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND));
        Long orderId = getOrderId(paymentEntity);

        if (paymentEntity.getStatus() == PaymentStatus.CANCELLED) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_ALREADY_CANCELLED);
        }

        if (paymentEntity.getStatus() == PaymentStatus.REFUND_PENDING) {
            return;
        }

        if (paymentEntity.getStatus() != PaymentStatus.PAID) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_CANNOT_REFUND);
        }

        if (orderStatePort.getOrderState(orderId) != OrderStatus.PAID) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_CANNOT_REFUND);
        }

        paymentEntity.paymentStatusChange(PaymentStatus.REFUND_PENDING);
        orderStatePort.changeOrderState(orderId, OrderStatus.REFUND_PENDING);
    }

    @Transactional
    public void rollbackRefundPending(Long paymentId, Long memberId) {
        PaymentEntity paymentEntity = paymentEntityRepository
                .findByIdAndMemberId(paymentId, memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND));
        Long orderId = paymentEntity.getOrderId();

        if (paymentEntity.getStatus() == PaymentStatus.REFUND_PENDING) {
            paymentEntity.paymentStatusChange(PaymentStatus.PAID);
            if (orderId != null && orderStatePort.getOrderState(orderId) == OrderStatus.REFUND_PENDING) {
                orderStatePort.changeOrderState(orderId, OrderStatus.PAID);
            }
        }
    }

    @Transactional
    public void confirmRefund(Long paymentId, Long memberId) {
        PaymentEntity paymentEntity = paymentEntityRepository
                .findByIdAndMemberId(paymentId, memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND));
        Long orderId = paymentEntity.getOrderId();

        if (paymentEntity.getStatus() == PaymentStatus.CANCELLED) {
            if (orderId == null) {
                return;
            }
            try {
                orderRefundPort.completeRefund(orderId, REFUND_COMPLETED_REASON);
            } catch (BusinessException e) {
                log.error("[Refund] 결제는 취소 상태지만 주문 보정 실패 paymentId={}, memberId={}",
                        paymentId, memberId, e);
                enqueueRecovery(paymentEntity, paymentId, memberId, e);
                throw e;
            }
            return;
        }

        if (paymentEntity.getStatus() != PaymentStatus.REFUND_PENDING
                && paymentEntity.getStatus() != PaymentStatus.PAID) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_CANNOT_REFUND);
        }

        paymentEntity.paymentStatusChange(PaymentStatus.CANCELLED);
        if (orderId == null) {
            throw new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND);
        }
        try {
            orderRefundPort.completeRefund(orderId, REFUND_COMPLETED_REASON);
        } catch (BusinessException e) {
            log.error("[Refund] 외부 환불은 완료됐지만 주문 취소 보정 실패 paymentId={}, memberId={}",
                    paymentId, memberId, e);
            enqueueRecovery(paymentEntity, paymentId, memberId, e);
            throw e;
        }
    }

    private static Long getOrderId(PaymentEntity paymentEntity) {
        Long orderId = paymentEntity.getOrderId();
        if (orderId == null) {
            throw new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND);
        }
        return orderId;
    }

    private void enqueueRecovery(PaymentEntity paymentEntity, Long paymentId, Long memberId, BusinessException e) {
        Long orderId = paymentEntity.getOrderId();
        recoveryQueueService.enqueue(paymentId, memberId, orderId, e.getErrorCode().getCode());
    }
}
