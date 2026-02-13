package kwh.cofshop.order.repository.custom;

import kwh.cofshop.order.dto.response.OrderResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;


public interface OrderRepositoryCustom {

    Page<OrderResponseDto> findOrderListById(Long id, Pageable pageable);

    Optional<OrderResponseDto> findByOrderIdWithItemsAndMemberId(Long orderId, Long memberId);

    Page<OrderResponseDto> findAllOrders(Pageable pageable);
}
