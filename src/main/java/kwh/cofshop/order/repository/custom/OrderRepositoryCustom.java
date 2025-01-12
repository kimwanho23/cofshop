package kwh.cofshop.order.repository.custom;

import kwh.cofshop.order.dto.response.OrderResponseDto;

import java.util.List;

public interface OrderRepositoryCustom {

    List<OrderResponseDto> findOrderListByEmail(String email);

    OrderResponseDto findByOrderIdWithItems(Long orderId);
}
