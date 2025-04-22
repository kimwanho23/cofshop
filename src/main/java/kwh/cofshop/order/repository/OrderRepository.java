package kwh.cofshop.order.repository;

import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.repository.custom.OrderRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, OrderRepositoryCustom {
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
