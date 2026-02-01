package kwh.cofshop.order.repository.custom;

import kwh.cofshop.order.dto.response.OrderResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface OrderRepositoryCustom {

    Page<OrderResponseDto> findOrderListById(Long id, Pageable pageable);

    OrderResponseDto findByOrderIdWithItems(Long orderId);

    Page<OrderResponseDto> findAllOrders(Pageable pageable);
}
