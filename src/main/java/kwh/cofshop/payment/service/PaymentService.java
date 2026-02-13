package kwh.cofshop.payment.service;

import kwh.cofshop.argumentResolver.DistributedLock;
import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.repository.OrderRepository;
import kwh.cofshop.payment.client.portone.PortOneCancellation;
import kwh.cofshop.payment.client.portone.PortOneClientException;
import kwh.cofshop.payment.client.portone.PortOnePayment;
import kwh.cofshop.payment.client.portone.PortOnePaymentClient;
import kwh.cofshop.payment.domain.PaymentEntity;
import kwh.cofshop.payment.domain.PaymentStatus;
import kwh.cofshop.payment.dto.PaymentPrepareRequestDto;
import kwh.cofshop.payment.dto.PaymentProviderResponseDto;
import kwh.cofshop.payment.dto.PaymentRefundRequestDto;
import kwh.cofshop.payment.dto.PaymentResponseDto;
import kwh.cofshop.payment.dto.PaymentVerifyRequestDto;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final String PORTONE_PAYMENT_STATUS_CANCELLED = "CANCELLED";
    private static final String PORTONE_CANCELLATION_STATUS_FAILED = "FAILED";
    private static final String DEFAULT_REFUND_REASON = "User requested refund";

    private final OrderRepository orderRepository;
    private final PaymentEntityRepository paymentEntityRepository;
    private final PortOnePaymentClient portOnePaymentClient;

    public PaymentProviderResponseDto getPaymentByImpUid(Long memberId, String impUid) {
        if (impUid == null || impUid.isBlank()) {
            throw new BadRequestException(BadRequestErrorCode.INVALID_IMP_UID);
        }

        PaymentEntity paymentEntity = paymentEntityRepository.findByImpUidAndMember_Id(impUid, memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND));

        PortOnePayment payment = fetchPaymentByMerchantUid(paymentEntity.getMerchantUid());
        if (!impUid.equals(payment.transactionId())) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_UID_DISCREPANCY);
        }

        return PaymentProviderResponseDto.from(payment);
    }

    @DistributedLock(keyName = "'payment:' + #orderId")
    @Transactional
    public PaymentResponseDto createPaymentRequest(Long memberId, Long orderId, PaymentPrepareRequestDto requestDto) {
        Order order = orderRepository.findByIdAndMember_Id(orderId, memberId).orElseThrow(
                () -> new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND)
        );

        order.pay();

        PaymentEntity currentPayment = PaymentEntity.createPayment(
                order, requestDto.getPgProvider(), requestDto.getPayMethod()
        );
        paymentEntityRepository.save(currentPayment);
        return PaymentResponseDto.from(currentPayment);
    }

    @Transactional
    public void verifyPayment(Long memberId, Long paymentEntityId, PaymentVerifyRequestDto requestDto) {
        PaymentEntity paymentEntity = paymentEntityRepository.findByIdAndMember_Id(paymentEntityId, memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND));

        PortOnePayment payment = fetchPaymentByMerchantUid(requestDto.getMerchantUid());
        validatePayment(payment, requestDto, paymentEntity);

        Long paidAmount = payment.paidAmount();
        if (paidAmount == null) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY);
        }

        if (!paymentEntity.getPrice().equals(paidAmount)) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY);
        }

        paymentEntity.paymentSuccess(
                payment.transactionId(),
                payment.pgTxId(),
                paidAmount,
                LocalDateTime.now()
        );
        paymentEntity.getOrder().changeOrderState(OrderState.PAID);
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

    @DistributedLock(keyName = "'refund:' + #paymentId")
    @Transactional
    public void refundPayment(Long paymentId, Long memberId, PaymentRefundRequestDto requestDto) {
        PaymentEntity paymentEntity = paymentEntityRepository
                .findByIdAndMember_Id(paymentId, memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND));

        if (paymentEntity.getStatus().equals(PaymentStatus.CANCELLED)) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_ALREADY_CANCELLED);
        }

        if (paymentEntity.getStatus() != PaymentStatus.PAID) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_CANNOT_REFUND);
        }

        if (requestDto.getAmount() != null) {
            Long paidAmount = paymentEntity.getPaidAmount();
            if (paidAmount == null || !paidAmount.equals(requestDto.getAmount())) {
                throw new BusinessException(BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY);
            }
        }

        PortOneCancellation cancellation = cancelPayment(paymentEntity.getMerchantUid());
        if (PORTONE_CANCELLATION_STATUS_FAILED.equalsIgnoreCase(cancellation.status())) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_REFUND_FAIL);
        }

        PortOnePayment payment = fetchPaymentByMerchantUid(paymentEntity.getMerchantUid());
        if (!PORTONE_PAYMENT_STATUS_CANCELLED.equalsIgnoreCase(payment.status())) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_REFUND_FAIL);
        }

        paymentEntity.paymentStatusChange(PaymentStatus.CANCELLED);
        paymentEntity.getOrder().changeOrderState(OrderState.CANCELLED);
    }

    private PortOnePayment fetchPaymentByMerchantUid(String merchantUid) {
        try {
            return portOnePaymentClient.getPayment(merchantUid);
        } catch (PortOneClientException e) {
            log.warn("PortOne payment 조회 실패: {}", e.getMessage());
            throw new BusinessException(BusinessErrorCode.PAYMENT_PROVIDER_ERROR);
        }
    }

    private PortOneCancellation cancelPayment(String merchantUid) {
        try {
            return portOnePaymentClient.cancelPayment(merchantUid, DEFAULT_REFUND_REASON);
        } catch (PortOneClientException e) {
            log.warn("PortOne payment 환불 실패: {}", e.getMessage());
            throw new BusinessException(BusinessErrorCode.PAYMENT_PROVIDER_ERROR);
        }
    }
}
