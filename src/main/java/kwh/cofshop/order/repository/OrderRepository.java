package kwh.cofshop.order.repository;

import jakarta.persistence.LockModeType;
import kwh.cofshop.order.domain.OrderRefundRequestStatus;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.repository.custom.OrderRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :orderId")
    Optional<Order> findByIdWithLock(@Param("orderId") Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :orderId AND o.member.id = :memberId")
    Optional<Order> findByIdAndMemberIdWithLock(@Param("orderId") Long orderId, @Param("memberId") Long memberId);

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

    @Query("""
            SELECT o.id
            FROM Order o
            WHERE o.refundRequestStatus = :refundStatus
              AND o.orderState IN :states
              AND o.refundProcessedAt <= :approvedBefore
            ORDER BY o.refundProcessedAt ASC
            """)
    List<Long> findPendingApprovedRefundOrderIds(@Param("refundStatus") OrderRefundRequestStatus refundStatus,
                                                 @Param("states") List<OrderState> states,
                                                 @Param("approvedBefore") LocalDateTime approvedBefore,
                                                 Pageable pageable);

    @Query("""
            SELECT COUNT(o)
            FROM Order o
            WHERE o.refundRequestStatus = :refundStatus
              AND o.orderState IN :states
              AND o.refundProcessedAt <= :approvedBefore
            """)
    long countPendingApprovedRefunds(@Param("refundStatus") OrderRefundRequestStatus refundStatus,
                                     @Param("states") List<OrderState> states,
                                     @Param("approvedBefore") LocalDateTime approvedBefore);

    @Query("SELECT o.orderState FROM Order o WHERE o.id = :orderId")
    Optional<OrderState> findOrderStateById(@Param("orderId") Long orderId);

    @Query("SELECT o.refundRequestStatus FROM Order o WHERE o.id = :orderId")
    Optional<OrderRefundRequestStatus> findRefundRequestStatusById(@Param("orderId") Long orderId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Order o
            SET o.orderState = :targetState
            WHERE o.id = :orderId
              AND o.orderState IN :currentStates
            """)
    int updateOrderStateIfCurrentIn(@Param("orderId") Long orderId,
                                    @Param("currentStates") List<OrderState> currentStates,
                                    @Param("targetState") OrderState targetState);

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
