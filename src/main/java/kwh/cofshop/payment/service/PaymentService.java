package kwh.cofshop.payment.service;

import kwh.cofshop.global.annotation.DistributedLock;
import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.order.api.OrderPaymentPrepareInfo;
import kwh.cofshop.order.api.OrderPaymentPreparePort;
import kwh.cofshop.order.api.OrderStatus;
import kwh.cofshop.order.api.OrderStatePort;
import kwh.cofshop.payment.client.portone.PortOneCancellation;
import kwh.cofshop.payment.client.portone.PortOnePayment;
import kwh.cofshop.payment.domain.PaymentEntity;
import kwh.cofshop.payment.domain.PaymentStatus;
import kwh.cofshop.payment.dto.request.PaymentPrepareRequestDto;
import kwh.cofshop.payment.dto.response.PaymentProviderResponseDto;
import kwh.cofshop.payment.dto.request.PaymentRefundRequestDto;
import kwh.cofshop.payment.dto.response.PaymentResponseDto;
import kwh.cofshop.payment.dto.request.PaymentVerifyRequestDto;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String PORTONE_PAYMENT_STATUS_PAID = "PAID";
    private static final String PORTONE_PAYMENT_STATUS_CANCELLED = "CANCELLED";
    private static final String PORTONE_CANCELLATION_STATUS_FAILED = "FAILED";

    private final OrderPaymentPreparePort orderPaymentPreparePort;
    private final OrderStatePort orderStatePort;
    private final PaymentRefundTxService paymentRefundTxService;
    private final PaymentEntityRepository paymentEntityRepository;
    private final PaymentProviderService paymentProviderService;

    public PaymentProviderResponseDto getPaymentByImpUid(Long memberId, String impUid) {
        if (impUid == null || impUid.isBlank()) {
            throw new BadRequestException(BadRequestErrorCode.INVALID_IMP_UID);
        }

        PaymentEntity paymentEntity = paymentEntityRepository.findByImpUidAndMemberId(impUid, memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND));

        PortOnePayment payment = paymentProviderService.getPayment(paymentEntity.getMerchantUid());
        if (!impUid.equals(payment.transactionId())) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_UID_DISCREPANCY);
        }

        return PaymentProviderResponseDto.from(payment);
    }

    @DistributedLock(keyName = "'payment:' + #orderId")
    @Transactional
    public PaymentResponseDto createPaymentRequest(Long memberId, Long orderId, PaymentPrepareRequestDto requestDto) {
        OrderPaymentPrepareInfo paymentPrepareInfo = orderPaymentPreparePort.prepare(orderId, memberId);

        PaymentEntity currentPayment = PaymentEntity.createPayment(
                paymentPrepareInfo.orderId(),
                paymentPrepareInfo.memberId(),
                paymentPrepareInfo.merchantUid(),
                paymentPrepareInfo.finalPrice(),
                paymentPrepareInfo.buyerEmail(),
                paymentPrepareInfo.buyerName(),
                paymentPrepareInfo.buyerTel(),
                requestDto.getPgProvider(),
                requestDto.getPayMethod()
        );
        paymentEntityRepository.save(currentPayment);
        return PaymentResponseDto.from(currentPayment);
    }

    @Transactional
    public void verifyPayment(Long memberId, Long paymentEntityId, PaymentVerifyRequestDto requestDto) {
        PaymentEntity paymentEntity = paymentEntityRepository.findByIdAndMemberId(paymentEntityId, memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND));

        PortOnePayment payment = paymentProviderService.getPayment(requestDto.getMerchantUid());
        validatePayment(payment, requestDto, paymentEntity);

        Long paidAmount = payment.paidAmount();
        if (paidAmount == null) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY);
        }

        Long orderId = getOrderId(paymentEntity);
        OrderStatus orderStatus = orderStatePort.getOrderState(orderId);

        if (paymentEntity.getStatus() == PaymentStatus.PAID) {
            if (orderStatus == OrderStatus.CANCELLED) {
                throw new BusinessException(BusinessErrorCode.ORDER_ALREADY_CANCELLED);
            }

            if (!Objects.equals(paymentEntity.getImpUid(), payment.transactionId())) {
                throw new BusinessException(BusinessErrorCode.PAYMENT_UID_DISCREPANCY);
            }
            if (!Objects.equals(paymentEntity.getPaidAmount(), paidAmount)) {
                throw new BusinessException(BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY);
            }
            if (orderStatus == OrderStatus.WAITING_FOR_PAY || orderStatus == OrderStatus.PAYMENT_PENDING) {
                orderStatePort.changeOrderState(orderId, OrderStatus.PAID);
            }
            return;
        }

        if (!paymentEntity.getPrice().equals(paidAmount)) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY);
        }

        if (orderStatus == OrderStatus.CANCELLED) {
            throw new BusinessException(BusinessErrorCode.ORDER_ALREADY_CANCELLED);
        }
        if (orderStatus != OrderStatus.WAITING_FOR_PAY && orderStatus != OrderStatus.PAYMENT_PENDING) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_FAIL);
        }

        paymentEntity.paymentSuccess(
                payment.transactionId(),
                payment.pgTxId(),
                paidAmount,
                LocalDateTime.now()
        );
        orderStatePort.changeOrderState(orderId, OrderStatus.PAID);
    }

    private static void validatePayment(
            PortOnePayment payment,
            PaymentVerifyRequestDto requestDto,
            PaymentEntity paymentEntity) {
        if (payment == null) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND);
        }

        if (!requestDto.getMerchantUid().equals(payment.id())) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_UID_DISCREPANCY);
        }

        if (!paymentEntity.getMerchantUid().equals(payment.id())) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_UID_DISCREPANCY);
        }

        if (!requestDto.getImpUid().equals(payment.transactionId())) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_UID_DISCREPANCY);
        }

        if (!PORTONE_PAYMENT_STATUS_PAID.equalsIgnoreCase(payment.status())) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_FAIL);
        }

        if (!requestDto.getAmount().equals(payment.paidAmount())) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY);
        }
    }

    @DistributedLock(keyName = "'refund:' + #paymentId")
    public void refundPayment(Long paymentId, Long memberId, PaymentRefundRequestDto requestDto) {
        PaymentEntity paymentEntity = paymentEntityRepository
                .findByIdAndMemberId(paymentId, memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND));

        if (paymentEntity.getStatus().equals(PaymentStatus.CANCELLED)) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_ALREADY_CANCELLED);
        }

        if (paymentEntity.getStatus() == PaymentStatus.REFUND_PENDING) {
            completePendingRefund(paymentId, memberId, paymentEntity.getMerchantUid());
            return;
        }

        if (paymentEntity.getStatus() != PaymentStatus.PAID) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_CANNOT_REFUND);
        }

        Long paidAmount = paymentEntity.getPaidAmount();
        if (paidAmount == null || !paidAmount.equals(requestDto.getAmount())) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY);
        }

        Long orderId = getOrderId(paymentEntity);
        if (orderStatePort.getOrderState(orderId) != OrderStatus.PAID) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_CANNOT_REFUND);
        }

        paymentRefundTxService.markRefundPending(paymentId, memberId);

        PortOneCancellation cancellation;
        try {
            cancellation = paymentProviderService.cancelPayment(paymentEntity.getMerchantUid());
        } catch (BusinessException e) {
            if (paymentProviderService.isPaymentCancelled(paymentEntity.getMerchantUid())) {
                paymentRefundTxService.confirmRefund(paymentId, memberId);
                return;
            }
            paymentRefundTxService.rollbackRefundPending(paymentId, memberId);
            throw e;
        }

        if (PORTONE_CANCELLATION_STATUS_FAILED.equalsIgnoreCase(cancellation.status())) {
            paymentRefundTxService.rollbackRefundPending(paymentId, memberId);
            throw new BusinessException(BusinessErrorCode.PAYMENT_REFUND_FAIL);
        }

        PortOnePayment payment = paymentProviderService.getPayment(paymentEntity.getMerchantUid());
        if (!PORTONE_PAYMENT_STATUS_CANCELLED.equalsIgnoreCase(payment.status())) {
            paymentRefundTxService.rollbackRefundPending(paymentId, memberId);
            throw new BusinessException(BusinessErrorCode.PAYMENT_REFUND_FAIL);
        }

        paymentRefundTxService.confirmRefund(paymentId, memberId);
    }

    private static Long getOrderId(PaymentEntity paymentEntity) {
        Long orderId = paymentEntity.getOrderId();
        if (orderId == null) {
            throw new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND);
        }
        return orderId;
    }

    private void completePendingRefund(Long paymentId, Long memberId, String merchantUid) {
        PortOnePayment payment = paymentProviderService.getPayment(merchantUid);
        if (!PORTONE_PAYMENT_STATUS_CANCELLED.equalsIgnoreCase(payment.status())) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_REFUND_FAIL);
        }
        paymentRefundTxService.confirmRefund(paymentId, memberId);
    }
}
