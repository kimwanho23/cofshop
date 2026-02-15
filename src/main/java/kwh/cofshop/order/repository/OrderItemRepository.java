package kwh.cofshop.order.repository;

import kwh.cofshop.order.domain.OrderItem;
import kwh.cofshop.order.repository.custom.OrderItemRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long>, OrderItemRepositoryCustom {
}
