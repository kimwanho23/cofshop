package kwh.cofshop.payment.service;

import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import kwh.cofshop.argumentResolver.DistributedLock;
import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.repository.OrderRepository;
import kwh.cofshop.payment.domain.PaymentEntity;
import kwh.cofshop.payment.domain.PaymentStatus;
import kwh.cofshop.payment.dto.PaymentPrepareRequestDto;
import kwh.cofshop.payment.dto.PaymentRefundRequestDto;
import kwh.cofshop.payment.dto.PaymentResponseDto;
import kwh.cofshop.payment.dto.PaymentVerifyRequestDto;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentEntityRepository paymentEntityRepository;
    private final IamportClient iamportClient;

    public Payment getPaymentByImpUid(String impUid) {
        if (impUid == null || impUid.isBlank()) {
            throw new BadRequestException(BadRequestErrorCode.INVALID_IMP_UID);
        }

        try {
            IamportResponse<Payment> response = iamportClient.paymentByImpUid(impUid);
            return response.getResponse();
        } catch (IamportResponseException | IOException e) {
            log.warn("Iamport API 호출 실패: {}", e.getMessage());
            throw new BadRequestException(BadRequestErrorCode.BAD_REQUEST);
        }
    }

    @DistributedLock(keyName = "'payment:' + #orderId")
    @Transactional
    public PaymentResponseDto createPaymentRequest(Long orderId, PaymentPrepareRequestDto requestDto) {
        Order order = orderRepository.findById(orderId).orElseThrow(
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
    public PaymentResponseDto createPaymentRequestTest(Long orderId, PaymentPrepareRequestDto requestDto) {
        Order order = orderRepository.findById(orderId).orElseThrow(
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
    public void verifyPayment(Long paymentEntityId, PaymentVerifyRequestDto requestDto) {
        PaymentEntity paymentEntity = paymentEntityRepository.findById(paymentEntityId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND));

        IamportResponse<Payment> paymentResponse;
        try {
            paymentResponse = iamportClient.paymentByImpUid(requestDto.getImpUid());
        } catch (IamportResponseException | IOException e) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND);
        }

        Payment payment = getPayment(requestDto, paymentResponse, paymentEntity);

        long paidAmount = payment.getAmount().longValue();
        if (!requestDto.getAmount().equals(paidAmount)) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY);
        }

        if (!paymentEntity.getPrice().equals(paidAmount)) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY);
        }

        paymentEntity.paymentSuccess(
                payment.getImpUid(),
                payment.getPgTid(),
                paidAmount,
                LocalDateTime.now()
        );
    }

    private static Payment getPayment(
            PaymentVerifyRequestDto requestDto,
            IamportResponse<Payment> paymentResponse,
            PaymentEntity paymentEntity
    ) {
        Payment payment = paymentResponse.getResponse();
        if (payment == null) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND);
        }

        if (!payment.getMerchantUid().equals(requestDto.getMerchantUid())) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_UID_DISCREPANCY);
        }

        if (!payment.getMerchantUid().equals(paymentEntity.getMerchantUid())) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_UID_DISCREPANCY);
        }

        return payment;
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

        IamportResponse<Payment> paymentResponse;
        try {
            CancelData cancelData = new CancelData(paymentEntity.getImpUid(), true);
            paymentResponse = iamportClient.cancelPaymentByImpUid(cancelData);
        } catch (IamportResponseException | IOException e) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND);
        }

        if (paymentResponse.getResponse() == null || !"cancelled".equals(
                paymentResponse.getResponse().getStatus())) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_REFUND_FAIL);
        }

        paymentEntity.paymentStatusChange(PaymentStatus.CANCELLED);
        /*
        OrderState와 PaymentState 상태 불일치
        각 State를 보고 사용자 처리 필요
         */
    }
}
