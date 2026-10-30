package kwh.cofshop.order.service;

import kwh.cofshop.item.api.ReviewEligibilityPort;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class ReviewEligibilityPortAdapter implements ReviewEligibilityPort {

    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean hasCompletedPurchase(Long memberId, Long itemId) {
        return orderItemRepository.existsPurchase(memberId, itemId, OrderState.COMPLETED);
    }
}
