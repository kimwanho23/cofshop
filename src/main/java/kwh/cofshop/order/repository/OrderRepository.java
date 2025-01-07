package kwh.cofshop.order.repository;

import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.repository.custom.OrderRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, OrderRepositoryCustom {
}
