package kwh.cofshop.order.repository;

import kwh.cofshop.order.domain.OrderItem;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.repository.custom.OrderItemRepositoryCustom;
import kwh.cofshop.order.repository.projection.ItemPurchasedQuantityProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long>, OrderItemRepositoryCustom {

    @Query("""
            SELECT COUNT(oi) > 0
            FROM OrderItem oi
            WHERE oi.order.member.id = :memberId
              AND oi.item.id = :itemId
              AND oi.order.orderState = :state
            """)
    boolean existsPurchase(@Param("memberId") Long memberId,
                           @Param("itemId") Long itemId,
                           @Param("state") OrderState state);

    @Query("""
            SELECT oi.item.id AS itemId, SUM(oi.quantity) AS quantity
            FROM OrderItem oi
            WHERE oi.order.member.id = :memberId
              AND oi.item.id IN :itemIds
              AND oi.order.orderState <> :excludedState
            GROUP BY oi.item.id
            """)
    List<ItemPurchasedQuantityProjection> sumPurchasedQuantities(@Param("memberId") Long memberId,
                                                                 @Param("itemIds") Collection<Long> itemIds,
                                                                 @Param("excludedState") OrderState excludedState);
}
