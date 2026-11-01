package kwh.cofshop.order.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.member.event.MemberWithdrawalEvent;
import kwh.cofshop.order.api.OrderMemberWithdrawnEvent;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.OrderPersonalArchive;
import kwh.cofshop.order.domain.OrderRefundRequestStatus;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.repository.OrderPersonalArchiveRepository;
import kwh.cofshop.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderWithdrawalService {

    // 계약·결제·공급 기록 보존 기간 (전자상거래법 시행령 제6조)
    private static final int RETENTION_YEARS = 5;

    private static final List<OrderState> FINISHED_STATES = List.of(OrderState.COMPLETED, OrderState.CANCELLED);
    private static final List<OrderRefundRequestStatus> REFUND_IN_PROGRESS =
            List.of(OrderRefundRequestStatus.REQUESTED, OrderRefundRequestStatus.APPROVED);

    private final OrderRepository orderRepository;
    private final OrderPersonalArchiveRepository archiveRepository;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    @Transactional
    public void archivePersonalInfo(MemberWithdrawalEvent event) {
        if (orderRepository.existsActiveOrder(event.memberId(), FINISHED_STATES, REFUND_IN_PROGRESS)) {
            throw new BusinessException(BusinessErrorCode.MEMBER_HAS_ACTIVE_ORDERS);
        }

        LocalDateTime now = LocalDateTime.now();
        for (Order order : orderRepository.findAllByMember_Id(event.memberId())) {
            LocalDateTime expiresAt = order.getOrderDate().plusYears(RETENTION_YEARS);
            if (expiresAt.isAfter(now)) {
                archiveRepository.save(OrderPersonalArchive.of(
                        order, event.email(), event.memberName(), event.tel(), now, expiresAt));
            }
            order.clearPersonalInfo();
        }
        eventPublisher.publishEvent(new OrderMemberWithdrawnEvent(event.memberId()));
    }

    @Transactional
    public int deleteExpiredArchives() {
        return archiveRepository.deleteExpired(LocalDateTime.now());
    }
}
