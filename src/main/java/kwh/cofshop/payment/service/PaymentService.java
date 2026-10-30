package kwh.cofshop.payment.service;

import kwh.cofshop.global.annotation.DistributedLock;
import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.order.api.OrderPaymentPrepareInfo;
import kwh.cofshop.order.api.OrderPaymentPreparePort;
import kwh.cofshop.order.api.OrderRefundPort;
import kwh.cofshop.order.api.OrderStatus;
import kwh.cofshop.order.api.OrderStatePort;
import kwh.cofshop.payment.client.portone.PortOneCancellation;
import kwh.cofshop.payment.client.portone.PortOnePayment;
import kwh.cofshop.payment.domain.PaymentEntity;
import kwh.cofshop.payment.domain.PaymentMethod;
import kwh.cofshop.payment.domain.PGProvider;
import kwh.cofshop.payment.domain.PaymentStatus;
import kwh.cofshop.payment.dto.request.PaymentPrepareRequestDto;
import kwh.cofshop.payment.dto.response.PaymentProviderResponseDto;
import kwh.cofshop.payment.dto.request.PaymentRefundRequestDto;
import kwh.cofshop.payment.dto.response.PaymentResponseDto;
import kwh.cofshop.payment.dto.request.PaymentVerifyRequestDto;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import kwh.cofshop.payment.repository.projection.PaymentProviderLookupProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String PORTONE_PAYMENT_STATUS_PAID = "PAID";
    private static final String PORTONE_PAYMENT_STATUS_CANCELLED = "CANCELLED";
    private static final String PORTONE_CANCELLATION_STATUS_FAILED = "FAILED";

    private final OrderPaymentPreparePort orderPaymentPreparePort;
    private final OrderStatePort orderStatePort;
    private final OrderRefundPort orderRefundPort;
    private final PaymentRefundTxService paymentRefundTxService;
    private final PaymentVerificationTxService paymentVerificationTxService;
    private final PaymentEntityRepository paymentEntityRepository;
    private final PaymentProviderService paymentProviderService;

    public PaymentProviderResponseDto getPaymentByImpUid(Long memberId, String impUid) {
        if (impUid == null || impUid.isBlank()) {
            throw new BadRequestException(BadRequestErrorCode.INVALID_IMP_UID);
        }

        PaymentProviderLookupProjection paymentLookup = paymentEntityRepository
                .findByImpUidAndMemberId(impUid, memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND));

        PortOnePayment payment = paymentProviderService.getPayment(paymentLookup.getMerchantUid());
        if (!impUid.equals(payment.transactionId())) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_UID_DISCREPANCY);
        }

        return PaymentProviderResponseDto.from(payment);
    }

    @DistributedLock(keyName = "'order:' + #orderId")
    @Transactional
    public PaymentCreateResult createPaymentRequest(Long memberId, Long orderId, PaymentPrepareRequestDto requestDto) {
        String normalizedPgProvider = normalizePgProvider(requestDto.getPgProvider());
        String normalizedPayMethod = normalizePayMethod(requestDto.getPayMethod());

        PaymentEntity existingPayment = paymentEntityRepository.findByOrderId(orderId).orElse(null);
        if (existingPayment != null) {
            if (!existingPayment.getMemberId().equals(memberId)) {
                throw new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND);
            }
            return PaymentCreateResult.reused(PaymentResponseDto.from(existingPayment));
        }

        OrderPaymentPrepareInfo paymentPrepareInfo = orderPaymentPreparePort.prepare(orderId, memberId);

        PaymentEntity currentPayment = PaymentEntity.createPayment(
                paymentPrepareInfo.orderId(),
                paymentPrepareInfo.memberId(),
                paymentPrepareInfo.merchantUid(),
                paymentPrepareInfo.finalPrice(),
                paymentPrepareInfo.buyerEmail(),
                paymentPrepareInfo.buyerName(),
                paymentPrepareInfo.buyerTel(),
                normalizedPgProvider,
                normalizedPayMethod
        );
        try {
            paymentEntityRepository.save(currentPayment);
        } catch (DataIntegrityViolationException e) {
            PaymentEntity duplicatedPayment = paymentEntityRepository.findByOrderId(orderId)
                    .orElseThrow(() -> e);
            if (!duplicatedPayment.getMemberId().equals(memberId)) {
                throw new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND);
            }
            return PaymentCreateResult.reused(PaymentResponseDto.from(duplicatedPayment));
        }
        return PaymentCreateResult.created(PaymentResponseDto.from(currentPayment));
    }

    private String normalizePgProvider(String rawProvider) {
        if (!PGProvider.isSupported(rawProvider)) {
            throw new BadRequestException(BadRequestErrorCode.UNSUPPORTED_PG_PROVIDER);
        }
        return PGProvider.normalize(rawProvider);
    }

    private String normalizePayMethod(String rawMethod) {
        if (!PaymentMethod.isSupported(rawMethod)) {
            throw new BadRequestException(BadRequestErrorCode.UNSUPPORTED_PAY_METHOD);
        }
        return PaymentMethod.normalize(rawMethod);
    }

    @DistributedLock(keyName = "'payment:' + #paymentEntityId")
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

        if (isAlreadyCancelledVerification(paymentEntity, payment, paidAmount, orderStatus)) {
            return;
        }

        if (orderStatus == OrderStatus.CANCELLED
                && PORTONE_PAYMENT_STATUS_CANCELLED.equalsIgnoreCase(payment.status())) {
            paymentVerificationTxService.syncCancelledOrderPayment(
                    paymentEntityId,
                    memberId,
                    payment.transactionId(),
                    payment.pgTxId(),
                    paidAmount
            );
            return;
        }

        if (!PORTONE_PAYMENT_STATUS_PAID.equalsIgnoreCase(payment.status())) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_FAIL);
        }

        if (orderStatus == OrderStatus.CANCELLED) {
            settleCancelledOrderPayment(
                    paymentEntityId,
                    memberId,
                    paymentEntity.getMerchantUid(),
                    payment.transactionId(),
                    payment.pgTxId(),
                    paidAmount
            );
            return;
        }

        paymentVerificationTxService.confirmPaidPayment(
                paymentEntityId,
                memberId,
                payment.transactionId(),
                payment.pgTxId(),
                paidAmount
        );
    }

    private void settleCancelledOrderPayment(
            Long paymentId,
            Long memberId,
            String merchantUid,
            String impUid,
            String pgTxId,
            Long paidAmount
    ) {
        PortOneCancellation cancellation = paymentProviderService.cancelPayment(merchantUid);
        if (PORTONE_CANCELLATION_STATUS_FAILED.equalsIgnoreCase(cancellation.status())) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_REFUND_FAIL);
        }

        PortOnePayment cancelledPayment = paymentProviderService.getPayment(merchantUid);
        if (!PORTONE_PAYMENT_STATUS_CANCELLED.equalsIgnoreCase(cancelledPayment.status())) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_REFUND_FAIL);
        }

        paymentVerificationTxService.settleCancelledOrderPayment(paymentId, memberId, impUid, pgTxId, paidAmount);
    }

    private boolean isAlreadyCancelledVerification(
            PaymentEntity paymentEntity,
            PortOnePayment payment,
            Long paidAmount,
            OrderStatus orderStatus) {
        if (orderStatus != OrderStatus.CANCELLED) {
            return false;
        }

        if (paymentEntity.getStatus() != PaymentStatus.CANCELLED) {
            return false;
        }

        if (!PORTONE_PAYMENT_STATUS_CANCELLED.equalsIgnoreCase(payment.status())) {
            return false;
        }

        if (paymentEntity.getImpUid() != null
                && !Objects.equals(paymentEntity.getImpUid(), payment.transactionId())) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_UID_DISCREPANCY);
        }

        if (paymentEntity.getPaidAmount() != null
                && !Objects.equals(paymentEntity.getPaidAmount(), paidAmount)) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY);
        }
        return true;
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

        if (!requestDto.getAmount().equals(payment.paidAmount())) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY);
        }
    }

    @DistributedLock(keyName = "'payment:' + #paymentId")
    public void refundPayment(Long paymentId, Long memberId, PaymentRefundRequestDto requestDto) {
        PaymentEntity paymentEntity = paymentEntityRepository
                .findByIdAndMemberId(paymentId, memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND));

        if (paymentEntity.getStatus().equals(PaymentStatus.CANCELLED)) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_ALREADY_CANCELLED);
        }

        if (paymentEntity.getStatus() == PaymentStatus.REFUND_PENDING) {
            resumePendingRefund(paymentId, memberId, paymentEntity);
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
        orderRefundPort.validateRefundApproved(orderId);

        paymentRefundTxService.markRefundPending(paymentId, memberId);
        executeRefundCancellation(paymentId, memberId, paymentEntity);
    }

    private static Long getOrderId(PaymentEntity paymentEntity) {
        Long orderId = paymentEntity.getOrderId();
        if (orderId == null) {
            throw new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND);
        }
        return orderId;
    }

    private void resumePendingRefund(Long paymentId, Long memberId, PaymentEntity paymentEntity) {
        String merchantUid = paymentEntity.getMerchantUid();
        PortOnePayment payment = paymentProviderService.getPayment(merchantUid);
        if (!PORTONE_PAYMENT_STATUS_CANCELLED.equalsIgnoreCase(payment.status())) {
            executeRefundCancellation(paymentId, memberId, paymentEntity);
            return;
        }
        paymentRefundTxService.confirmRefund(paymentId, memberId);
    }

    private void executeRefundCancellation(Long paymentId, Long memberId, PaymentEntity paymentEntity) {
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

    public record PaymentCreateResult(PaymentResponseDto response, boolean created) {

        public static PaymentCreateResult created(PaymentResponseDto response) {
            return new PaymentCreateResult(response, true);
        }

        public static PaymentCreateResult reused(PaymentResponseDto response) {
            return new PaymentCreateResult(response, false);
        }
    }
}
