package kwh.cofshop.order.repository.custom;

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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final OrderMapper orderMapper;


    @Override
    public Page<OrderResponseDto> findOrderListById(Long id, Pageable pageable) {
        QOrder order = QOrder.order;

        long total = countByMemberId(id);
        if (total == 0L) {
            return new PageImpl<>(List.of(), pageable, 0L);
        }

        List<Long> orderIds = queryFactory
                .select(order.id)
                .from(order)
                .where(order.member.id.eq(id))
                .orderBy(order.createDate.desc(), order.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        if (orderIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        List<OrderResponseDto> content = fetchOrdersWithItems(orderIds).stream()
                .map(orderMapper::toResponseDto)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Optional<OrderResponseDto> findByOrderIdWithItemsAndMemberId(Long orderId, Long memberId) {
        QOrder order = QOrder.order;
        QOrderItem orderItem = QOrderItem.orderItem;
        QMember member = QMember.member;
        QItem item = QItem.item;
        QItemOption itemOption = QItemOption.itemOption;

        Order fetchedOrder = queryFactory
                .select(order)
                .distinct()
                .from(order)
                .join(order.member, member)
                .leftJoin(order.orderItems, orderItem).fetchJoin()
                .leftJoin(orderItem.itemOption, itemOption).fetchJoin()
                .leftJoin(orderItem.item, item).fetchJoin()
                .where(order.id.eq(orderId), member.id.eq(memberId))
                .fetchOne();

        return Optional.ofNullable(fetchedOrder)
                .map(orderMapper::toResponseDto);

    }

    @Override
    public Page<OrderResponseDto> findAllOrders(Pageable pageable) {
        QOrder order = QOrder.order;

        long total = countAllOrders();
        if (total == 0L) {
            return new PageImpl<>(List.of(), pageable, 0L);
        }

        List<Long> orderIds = queryFactory
                .select(order.id)
                .from(order)
                .orderBy(order.createDate.desc(), order.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        if (orderIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        List<OrderResponseDto> content = fetchOrdersWithItems(orderIds).stream()
                .map(orderMapper::toResponseDto)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    private long countByMemberId(Long memberId) {
        QOrder order = QOrder.order;
        return Optional.ofNullable(
                queryFactory
                        .select(order.count())
                        .from(order)
                        .where(order.member.id.eq(memberId))
                        .fetchOne()
        ).orElse(0L);
    }

    private long countAllOrders() {
        QOrder order = QOrder.order;
        return Optional.ofNullable(
                queryFactory
                        .select(order.count())
                        .from(order)
                        .fetchOne()
        ).orElse(0L);
    }

    private List<Order> fetchOrdersWithItems(List<Long> orderIds) {
        QOrder order = QOrder.order;
        QOrderItem orderItem = QOrderItem.orderItem;
        QItem item = QItem.item;
        QItemOption itemOption = QItemOption.itemOption;

        List<Order> fetchedOrders = queryFactory
                .selectFrom(order)
                .distinct()
                .leftJoin(order.orderItems, orderItem).fetchJoin()
                .leftJoin(orderItem.itemOption, itemOption).fetchJoin()
                .leftJoin(orderItem.item, item).fetchJoin()
                .where(order.id.in(orderIds))
                .fetch();

        Map<Long, Order> orderById = fetchedOrders.stream()
                .collect(Collectors.toMap(
                        Order::getId,
                        o -> o,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        return orderIds.stream()
                .map(orderById::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
