package kwh.cofshop.order.repository.custom;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.item.domain.QItem;
import kwh.cofshop.item.domain.QItemOption;
import kwh.cofshop.member.domain.QMember;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.QOrder;
import kwh.cofshop.order.domain.QOrderItem;
import kwh.cofshop.order.dto.response.OrderResponseDto;
import kwh.cofshop.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepositoryCustom{

    private final JPAQueryFactory queryFactory;
    private final OrderMapper orderMapper;


    @Override
    public Page<OrderResponseDto> findOrderListById(Long id, Pageable pageable) {
        QOrder order = QOrder.order;
        QOrderItem orderItem = QOrderItem.orderItem;
        QMember member = QMember.member;
        QItemOption itemOption = QItemOption.itemOption;

        JPQLQuery<Order> query = queryFactory
                .select(order)
                .from(order)
                .join(order.member, member)
                .leftJoin(order.orderItems, orderItem)
                .leftJoin(orderItem.itemOption, itemOption)
                .where(member.id.eq(id))
                .orderBy(order.createDate.desc());

        QueryResults<Order> results = query.offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        List<OrderResponseDto> content = results.getResults()
                .stream()
                .map(orderMapper::toResponseDto)
                .toList();

        return new PageImpl<>(content, pageable, results.getTotal());
    }

    @Override
    public OrderResponseDto findByOrderIdWithItems(Long orderId) {
        QOrder order = QOrder.order;
        QOrderItem orderItem = QOrderItem.orderItem;
        QItem item = QItem.item;
        QItemOption itemOption = QItemOption.itemOption;

        Order fetchedOrder = queryFactory
                .select(order)
                .from(order)
                .join(order.orderItems, orderItem).fetchJoin()
                .join(orderItem.itemOption, itemOption).fetchJoin()
                .join(itemOption.item, item).fetchJoin()
                .where(order.id.eq(orderId))
                .fetchOne();

        return orderMapper.toResponseDto(fetchedOrder);

    }

    @Override
    public Page<OrderResponseDto> findAllOrders(Pageable pageable) {
        QOrder order = QOrder.order;
        QOrderItem orderItem = QOrderItem.orderItem;

        List<Order> orders = queryFactory
                .selectFrom(order)
                .leftJoin(order.orderItems, orderItem).fetchJoin()
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = Optional.ofNullable(queryFactory
                        .select(order.count())
                        .from(order)
                        .fetchOne())
                .orElse(0L);

        List<OrderResponseDto> content = orders.stream()
                .map(orderMapper::toResponseDto)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }
}
