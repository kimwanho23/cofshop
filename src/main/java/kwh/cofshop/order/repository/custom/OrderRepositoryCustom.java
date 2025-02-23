package kwh.cofshop.order.repository.custom;

import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.dto.response.OrderResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepositoryCustom {

    Page<OrderResponseDto> findOrderListById(Long id, Pageable pageable);

    OrderResponseDto findByOrderIdWithItems(Long orderId);

    Page<OrderResponseDto> findAllOrders(Pageable pageable);
}
