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
import io.micrometer.core.instrument.Metrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
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
        // 중복 생성은 락과 order_id 유니크 제약이 막음. 위반 시 트랜잭션이 이미 롤백 표시되므로 409로 응답하고 재요청에서 재사용
        paymentEntityRepository.save(currentPayment);
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

    // 고객의 검증 요청 없이 PG 상태로 결제를 맞춤. 결제 완료면 확정하고, 주문이 이미 취소됐으면 PG 취소까지 진행
    @DistributedLock(keyName = "'payment:' + #paymentId")
    public void reconcilePayment(Long paymentId) {
        PaymentEntity paymentEntity = paymentEntityRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND));
        if (paymentEntity.getStatus() != PaymentStatus.READY) {
            return;
        }

        PortOnePayment payment = paymentProviderService.findPayment(paymentEntity.getMerchantUid()).orElse(null);
        if (payment == null || !PORTONE_PAYMENT_STATUS_PAID.equalsIgnoreCase(payment.status())) {
            return;
        }

        Long paidAmount = payment.paidAmount();
        if (paidAmount == null) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY);
        }

        Long orderId = getOrderId(paymentEntity);
        if (orderStatePort.getOrderState(orderId) == OrderStatus.CANCELLED) {
            settleCancelledOrderPayment(
                    paymentId,
                    paymentEntity.getMemberId(),
                    paymentEntity.getMerchantUid(),
                    payment.transactionId(),
                    payment.pgTxId(),
                    paidAmount
            );
            log.warn("[Reconcile] 취소된 주문의 결제를 PG 취소 처리 paymentId={}, orderId={}", paymentId, orderId);
            Metrics.counter("payment.reconciliation.recovered", "result", "refunded").increment();
            return;
        }

        paymentVerificationTxService.confirmPaidPayment(
                paymentId,
                paymentEntity.getMemberId(),
                payment.transactionId(),
                payment.pgTxId(),
                paidAmount
        );
        log.warn("[Reconcile] 검증 요청이 없던 결제를 확정 paymentId={}, orderId={}", paymentId, orderId);
        Metrics.counter("payment.reconciliation.recovered", "result", "confirmed").increment();
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
        if (!orderStatePort.getOrderState(orderId).isRefundable()) {
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

    // PG가 취소 실패를 명확히 응답한 경우에만 되돌림. 타임아웃이나 처리 중이면 이미 환불됐을 수 있으므로
    // REFUND_PENDING을 유지하고, 재시도(ApprovedRefundRetryScheduler)에서 PG 상태로 확정
    private void executeRefundCancellation(Long paymentId, Long memberId, PaymentEntity paymentEntity) {
        PortOneCancellation cancellation;
        try {
            cancellation = paymentProviderService.cancelPayment(paymentEntity.getMerchantUid());
        } catch (BusinessException e) {
            if (paymentProviderService.isPaymentCancelled(paymentEntity.getMerchantUid())) {
                paymentRefundTxService.confirmRefund(paymentId, memberId);
                return;
            }
            throw e;
        }

        if (PORTONE_CANCELLATION_STATUS_FAILED.equalsIgnoreCase(cancellation.status())) {
            paymentRefundTxService.rollbackRefundPending(paymentId, memberId);
            throw new BusinessException(BusinessErrorCode.PAYMENT_REFUND_FAIL);
        }

        PortOnePayment payment = paymentProviderService.getPayment(paymentEntity.getMerchantUid());
        if (!PORTONE_PAYMENT_STATUS_CANCELLED.equalsIgnoreCase(payment.status())) {
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
