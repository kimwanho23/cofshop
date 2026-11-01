package kwh.cofshop.payment.service;

import kwh.cofshop.order.api.OrderMemberWithdrawnEvent;
import kwh.cofshop.payment.repository.PaymentEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 구매자 정보는 주문 모듈의 분리 보관 테이블에 남고, 결제 기록에는 금액과 PG 거래 번호만 남긴다
@Component
@RequiredArgsConstructor
public class PaymentWithdrawalListener {

    private final PaymentEntityRepository paymentEntityRepository;

    @EventListener
    @Transactional
    public void clearBuyerInfo(OrderMemberWithdrawnEvent event) {
        paymentEntityRepository.clearBuyerInfo(event.memberId());
    }
}
