package kwh.cofshop.order.repository;

import kwh.cofshop.order.domain.OrderRefundRequestStatus;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.repository.custom.OrderRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, OrderRepositoryCustom {
    Optional<Order> findByIdAndMember_Id(Long id, Long memberId);

    @Query("""
            SELECT o.id
            FROM Order o
            WHERE o.orderState IN :states
              AND o.orderDate <= :deadline
            ORDER BY o.orderDate ASC
            """)
    List<Long> findStaleOrderIdsForAutoCancel(@Param("states") List<OrderState> states,
                                               @Param("deadline") LocalDateTime deadline,
                                               Pageable pageable);

    @Query("SELECT o.orderState FROM Order o WHERE o.id = :orderId")
    Optional<OrderState> findOrderStateById(@Param("orderId") Long orderId);

    @Query("SELECT o.refundRequestStatus FROM Order o WHERE o.id = :orderId")
    Optional<OrderRefundRequestStatus> findRefundRequestStatusById(@Param("orderId") Long orderId);

    @Query(value = """
                SELECT COUNT(*)
                FROM orders
                WHERE order_year = :year AND order_month = :month
            """, nativeQuery = true)
    Long countByYearAndMonth(@Param("year") int year, @Param("month") int month);

    @Query(value = """
                SELECT COUNT(*)
                FROM orders
                WHERE YEAR(order_date) = :year AND MONTH(order_date) = :month
            """, nativeQuery = true)
    Long countByOrderDateYearAndMonth(@Param("year") int year, @Param("month") int month);
}
