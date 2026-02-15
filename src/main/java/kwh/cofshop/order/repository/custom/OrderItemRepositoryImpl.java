package kwh.cofshop.order.repository.custom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.QItem;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.domain.QOrder;
import kwh.cofshop.order.domain.QOrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderItemRepositoryImpl implements OrderItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Item> getPopularItems(int limit) {
        QOrderItem orderItem = QOrderItem.orderItem;
        QItem item = QItem.item;
        QOrder order = QOrder.order;

        return queryFactory
                .select(item)
                .from(orderItem)
                .join(orderItem.order, order)
                .join(orderItem.item, item)
                .where(order.orderState.in(
                        OrderState.PAID,
                        OrderState.PREPARING_FOR_SHIPMENT,
                        OrderState.SHIPPING,
                        OrderState.DELIVERED,
                        OrderState.COMPLETED
                ))
                .groupBy(item.id)
                .orderBy(orderItem.quantity.sum().desc())
                .limit(limit)
                .fetch();
    }
}
