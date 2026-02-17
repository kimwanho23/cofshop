package kwh.cofshop.order.service;

import kwh.cofshop.item.api.PopularItemPort;
import kwh.cofshop.order.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
class PopularItemPortAdapter implements PopularItemPort {

    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Long> getPopularItemIds(int limit) {
        return orderItemRepository.getPopularItemIds(limit);
    }
}
