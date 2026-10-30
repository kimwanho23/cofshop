package kwh.cofshop.payment.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.order.api.OrderStatus;
import kwh.cofshop.order.api.OrderStatePort;
import kwh.cofshop.payment.domain.PaymentEntity;
import kwh.cofshop.payment.domain.PaymentStatus;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentVerificationTxService {

    private final PaymentEntityRepository paymentEntityRepository;
    private final OrderStatePort orderStatePort;

    @Transactional
    public void confirmPaidPayment(Long paymentId, Long memberId, String impUid, String pgTxId, Long paidAmount) {
        PaymentEntity paymentEntity = findPaymentWithLock(paymentId, memberId);
        Long orderId = getOrderId(paymentEntity);
        OrderStatus orderStatus = orderStatePort.getOrderState(orderId);

        if (paymentEntity.getStatus() == PaymentStatus.CANCELLED) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_FAIL);
        }

        if (paymentEntity.getStatus() == PaymentStatus.PAID) {
            validatePaidConsistency(paymentEntity, impUid, paidAmount);
            if (orderStatus == OrderStatus.WAITING_FOR_PAY || orderStatus == OrderStatus.PAYMENT_PENDING) {
                orderStatePort.changeOrderState(orderId, OrderStatus.PAID);
            }
            return;
        }

        if (!Objects.equals(paymentEntity.getPrice(), paidAmount)) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY);
        }
        if (orderStatus != OrderStatus.WAITING_FOR_PAY && orderStatus != OrderStatus.PAYMENT_PENDING) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_FAIL);
        }

        paymentEntity.paymentSuccess(impUid, pgTxId, paidAmount, LocalDateTime.now());
        orderStatePort.changeOrderState(orderId, OrderStatus.PAID);
    }

    @Transactional
    public void settleCancelledOrderPayment(Long paymentId, Long memberId, String impUid, String pgTxId, Long paidAmount) {
        applyCancelledOrderPayment(paymentId, memberId, impUid, pgTxId, paidAmount);
    }

    @Transactional
    public void syncCancelledOrderPayment(Long paymentId, Long memberId, String impUid, String pgTxId, Long paidAmount) {
        applyCancelledOrderPayment(paymentId, memberId, impUid, pgTxId, paidAmount);
    }

    private void applyCancelledOrderPayment(
            Long paymentId,
            Long memberId,
            String impUid,
            String pgTxId,
            Long paidAmount
    ) {
        PaymentEntity paymentEntity = findPaymentWithLock(paymentId, memberId);

        if (paymentEntity.getStatus() == PaymentStatus.CANCELLED) {
            validateCancelledConsistency(paymentEntity, impUid, paidAmount);
            return;
        }

        if (paymentEntity.getStatus() == PaymentStatus.PAID) {
            validatePaidConsistency(paymentEntity, impUid, paidAmount);
        } else {
            if (!Objects.equals(paymentEntity.getPrice(), paidAmount)) {
                throw new BusinessException(BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY);
            }
            paymentEntity.paymentSuccess(impUid, pgTxId, paidAmount, LocalDateTime.now());
        }
        paymentEntity.paymentStatusChange(PaymentStatus.CANCELLED);
    }

    private PaymentEntity findPaymentWithLock(Long paymentId, Long memberId) {
        return paymentEntityRepository.findByIdAndMemberIdWithLock(paymentId, memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND));
    }

    private Long getOrderId(PaymentEntity paymentEntity) {
        Long orderId = paymentEntity.getOrderId();
        if (orderId == null) {
            throw new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND);
        }
        return orderId;
    }

    private void validatePaidConsistency(PaymentEntity paymentEntity, String impUid, Long paidAmount) {
        if (!Objects.equals(paymentEntity.getImpUid(), impUid)) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_UID_DISCREPANCY);
        }
        if (!Objects.equals(paymentEntity.getPaidAmount(), paidAmount)) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY);
        }
    }

    private void validateCancelledConsistency(PaymentEntity paymentEntity, String impUid, Long paidAmount) {
        if (paymentEntity.getImpUid() != null && !Objects.equals(paymentEntity.getImpUid(), impUid)) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_UID_DISCREPANCY);
        }
        if (paymentEntity.getPaidAmount() != null && !Objects.equals(paymentEntity.getPaidAmount(), paidAmount)) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY);
        }
    }
}
