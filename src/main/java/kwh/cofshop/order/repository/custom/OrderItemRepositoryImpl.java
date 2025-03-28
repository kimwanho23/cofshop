package kwh.cofshop.order.repository.custom;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.QItem;
import kwh.cofshop.item.domain.QItemOption;
import kwh.cofshop.member.domain.QMember;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.QOrder;
import kwh.cofshop.order.domain.QOrderItem;
import kwh.cofshop.order.dto.response.OrderItemResponseDto;
import kwh.cofshop.order.dto.response.OrderResponseDto;
import kwh.cofshop.order.mapper.OrderMapper;
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
