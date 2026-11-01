package kwh.cofshop.payment.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.payment.domain.PaymentEntity;
import kwh.cofshop.payment.domain.PaymentStatus;
import kwh.cofshop.payment.dto.request.PaymentRefundRequestDto;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 관리자 승인 후 환불 실행. PaymentService의 분산 락이 적용되도록 별도 빈으로 분리
@Service
@RequiredArgsConstructor
public class ApprovedRefundExecutor {

    private final PaymentEntityRepository paymentEntityRepository;
    private final PaymentService paymentService;
    private final PaymentRefundTxService paymentRefundTxService;

    public void execute(Long orderId) {
        PaymentEntity paymentEntity = paymentEntityRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.PAYMENT_NOT_FOUND));

        // PG 환불은 끝났고 주문 보정만 남은 경우. 재적재는 복구 큐에 맡김
        if (paymentEntity.getStatus() == PaymentStatus.CANCELLED) {
            paymentRefundTxService.confirmRefundForRecovery(paymentEntity.getId(), paymentEntity.getMemberId());
            return;
        }

        PaymentRefundRequestDto requestDto = new PaymentRefundRequestDto();
        requestDto.setAmount(paymentEntity.getPaidAmount());
        try {
            paymentService.refundPayment(paymentEntity.getId(), paymentEntity.getMemberId(), requestDto);
        } catch (BusinessException e) {
            if (e.getErrorCode() != BusinessErrorCode.PAYMENT_ALREADY_CANCELLED) {
                throw e;
            }
            // 조회 후 락을 얻기 전에 다른 요청이 환불을 끝낸 경우. 실패가 아니므로 주문 보정만 확인
            paymentRefundTxService.confirmRefundForRecovery(paymentEntity.getId(), paymentEntity.getMemberId());
        }
    }
}
