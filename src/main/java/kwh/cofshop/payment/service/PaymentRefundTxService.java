package kwh.cofshop.payment.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.service.OrderService;
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

    private final PaymentEntityRepository paymentEntityRepository;
    private final OrderService orderService;
    private final RefundCompensationRecoveryQueueService recoveryQueueService;

    @Transactional
    public void markRefundPending(Long paymentId, Long memberId) {
        PaymentEntity paymentEntity = paymentEntityRepository
                .findByIdAndMember_Id(paymentId, memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND));
        Order order = paymentEntity.getOrder();

        if (paymentEntity.getStatus() == PaymentStatus.CANCELLED) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_ALREADY_CANCELLED);
        }

        if (paymentEntity.getStatus() == PaymentStatus.REFUND_PENDING) {
            return;
        }

        if (paymentEntity.getStatus() != PaymentStatus.PAID) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_CANNOT_REFUND);
        }

        if (order == null || order.getOrderState() != OrderState.PAID) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_CANNOT_REFUND);
        }

        paymentEntity.paymentStatusChange(PaymentStatus.REFUND_PENDING);
        order.changeOrderState(OrderState.REFUND_PENDING);
    }

    @Transactional
    public void rollbackRefundPending(Long paymentId, Long memberId) {
        PaymentEntity paymentEntity = paymentEntityRepository
                .findByIdAndMember_Id(paymentId, memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND));
        Order order = paymentEntity.getOrder();

        if (paymentEntity.getStatus() == PaymentStatus.REFUND_PENDING) {
            paymentEntity.paymentStatusChange(PaymentStatus.PAID);
            if (order != null && order.getOrderState() == OrderState.REFUND_PENDING) {
                order.changeOrderState(OrderState.PAID);
            }
        }
    }

    @Transactional
    public void confirmRefund(Long paymentId, Long memberId) {
        PaymentEntity paymentEntity = paymentEntityRepository
                .findByIdAndMember_Id(paymentId, memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND));
        Order order = paymentEntity.getOrder();

        if (paymentEntity.getStatus() == PaymentStatus.CANCELLED) {
            if (order == null || order.getOrderState() == OrderState.CANCELLED) {
                return;
            }
            try {
                orderService.cancelAndRestore(order);
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
        try {
            orderService.cancelAndRestore(order);
        } catch (BusinessException e) {
            log.error("[Refund] 외부 환불은 완료됐지만 주문 취소 보정 실패 paymentId={}, memberId={}",
                    paymentId, memberId, e);
            enqueueRecovery(paymentEntity, paymentId, memberId, e);
            throw e;
        }
    }

    @Transactional
    public void applyRefund(Long paymentId, Long memberId) {
        confirmRefund(paymentId, memberId);
    }

    private void enqueueRecovery(PaymentEntity paymentEntity, Long paymentId, Long memberId, BusinessException e) {
        Long orderId = paymentEntity.getOrder() == null ? null : paymentEntity.getOrder().getId();
        recoveryQueueService.enqueue(paymentId, memberId, orderId, e.getErrorCode().getCode());
    }
}
