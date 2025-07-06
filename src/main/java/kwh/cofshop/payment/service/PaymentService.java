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
import kwh.cofshop.payment.dto.*;
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

    public Payment getPaymentByImpUid(String impUid){
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

    // 결제 요청 생성
    @DistributedLock(keyName = "'payment:' + #orderId")
    @Transactional
    public PaymentResponseDto createPaymentRequest(Long orderId, PaymentPrepareRequestDto requestDto) {
        // 1. 주문 정보 확인
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND)
        );

        // 2. 주문 상태 확인 & 주문 생성 - 결제 대기
        order.pay();

        // 3. 결제 정보 저장
        PaymentEntity currentPayment = PaymentEntity.
                createPayment(order, requestDto.getPgProvider(), requestDto.getPayMethod());
        paymentEntityRepository.save(currentPayment); // 결제 정보 저장

        return PaymentResponseDto.from(currentPayment);
    }

    // 결제 요청 생성
    @Transactional
    public PaymentResponseDto createPaymentRequestTest(Long orderId, PaymentPrepareRequestDto requestDto) {
        // 1. 주문 정보 확인
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new BusinessException(BusinessErrorCode.ORDER_NOT_FOUND)
        );

        // 2. 주문 상태 확인 & 주문 생성 - 결제 대기
        order.pay();

        // 3. 결제 정보 저장
        PaymentEntity currentPayment = PaymentEntity.
                createPayment(order, requestDto.getPgProvider(), requestDto.getPayMethod());
        paymentEntityRepository.save(currentPayment); // 결제 정보 저장

        return PaymentResponseDto.from(currentPayment);
    }

    // 결제 검증
    @Transactional
    public void verifyPayment(Long paymentEntityId, PaymentVerifyRequestDto requestDto) {
        IamportResponse<Payment> paymentResponse;

        PaymentEntity paymentEntity = paymentEntityRepository.findById(paymentEntityId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND));

        try {
            paymentResponse = iamportClient.paymentByImpUid(requestDto.getImpUid());
        } catch (IamportResponseException | IOException e) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND); // 중복 결제 검증
        }

        Payment payment = paymentResponse.getResponse();

        if (!payment.getMerchantUid().equals(requestDto.getMerchantUid())) { // 결제 UID 검증
            throw new BusinessException(BusinessErrorCode.PAYMENT_UID_DISCREPANCY);
        }

        if (payment.getAmount().intValue() != requestDto.getAmount()) { // 결제 금액 검증
            throw new BusinessException(BusinessErrorCode.PAYMENT_AMOUNT_DISCREPANCY);
        }

        // 검증이 제대로 이루어졌다면, 상태를 업데이트한다.
        paymentEntity.paymentSuccess(payment.getImpUid(), payment.getPgTid(),
                payment.getAmount().longValue(), LocalDateTime.now()
        );
    }

    // 환불 요청
    @DistributedLock(keyName = "'refund:' + #requestDto.merchantUid")
    @Transactional
    public void refundPayment(Long memberId, PaymentRefundRequestDto requestDto) {
        // 결제 내역 조회 및 검증
        PaymentEntity paymentEntity = paymentEntityRepository
                .findByMerchantUidAndMemberId(requestDto.getMerchantUid(), memberId);
        
        // 이미 취소된 결제는 Exception 발생
        if (paymentEntity.getStatus().equals(PaymentStatus.CANCELLED)){
            throw new BusinessException(BusinessErrorCode.PAYMENT_ALREADY_CANCELLED); // 이미 취소된 결제
        }

        // 결제된 건이 아니면 Exception
        if (paymentEntity.getStatus() != PaymentStatus.PAID) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_CANNOT_REFUND); //
        }

        // 아임포트 서버에서 결제 상태 조회
        IamportResponse<Payment> paymentResponse;

        // 아임포트에 환불 요청
        try {
            CancelData cancelData = new CancelData(paymentEntity.getImpUid(), true); // imp_uid, 전액 환불
            paymentResponse = iamportClient.cancelPaymentByImpUid(cancelData);
        } catch (IamportResponseException | IOException e) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND); // 임시 오류
        }

        // 환불 실패 확인
        if (paymentResponse.getResponse() == null || !"cancelled".equals(
                paymentResponse.getResponse().getStatus())) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_REFUND_FAIL);
        }

        // 3. 상태 업데이트
        paymentEntity.paymentStatusChange(PaymentStatus.CANCELLED); // 상태 변경

        /*
        OrderState와 PaymentState 상태 불일치 일 때?
        각각의 State를 보고 재실행 가능.
         */
    }

}

