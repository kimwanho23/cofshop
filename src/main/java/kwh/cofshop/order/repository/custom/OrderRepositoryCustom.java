package kwh.cofshop.order.repository.custom;

import kwh.cofshop.order.dto.response.OrderResponseDto;

public interface OrderRepositoryCustom {

    OrderResponseDto findByOrderIdWithItems(Long id);
}
