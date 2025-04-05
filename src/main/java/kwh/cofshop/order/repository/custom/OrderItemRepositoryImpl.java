package kwh.cofshop.order.repository.custom;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.QItem;

import kwh.cofshop.order.domain.QOrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderItemRepositoryImpl implements OrderItemRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Item> getPopularItems(int limit) {
        QOrderItem orderItem = QOrderItem.orderItem;
        QItem item = QItem.item;

        return queryFactory
                .select(item)
                .from(orderItem)
                .join(orderItem.item, item)
                .groupBy(item.id)
                .orderBy(orderItem.quantity.sum().desc()) // 판매량 순
                .limit(limit)
                .fetch();
    }
}
