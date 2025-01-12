package kwh.cofshop.order.repository.custom;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.item.domain.QItem;
import kwh.cofshop.item.domain.QItemOption;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.QMember;
import kwh.cofshop.member.dto.MemberResponseDto;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.OrderItem;
import kwh.cofshop.order.domain.QOrder;
import kwh.cofshop.order.domain.QOrderItem;
import kwh.cofshop.order.dto.request.OrdererRequestDto;
import kwh.cofshop.order.dto.response.OrderItemResponseDto;
import kwh.cofshop.order.dto.response.OrderResponseDto;
import kwh.cofshop.order.dto.response.OrdererResponseDto;
import kwh.cofshop.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepositoryCustom{

    private final JPAQueryFactory queryFactory;
    private final OrderMapper orderMapper;


    @Override
    public List<OrderResponseDto> findOrderListByEmail(String email) {
        QOrder order = QOrder.order;
        QOrderItem orderItem = QOrderItem.orderItem;
        QMember member = QMember.member;
        QItemOption itemOption = QItemOption.itemOption;

        List<Order> orders = queryFactory
                .selectDistinct(order)
                .from(order)
                .join(order.member, member).fetchJoin()
                .leftJoin(order.orderItems, orderItem).fetchJoin()
                .leftJoin(orderItem.itemOption, itemOption).fetchJoin()
                .where(member.email.eq(email))
                .orderBy(order.createDate.desc())
                .fetch();

        return orders.stream()
                .map(orderMapper::toResponseDto)
                .toList();
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
                .where(order.orderId.eq(orderId))
                .fetchOne();

        return orderMapper.toResponseDto(fetchedOrder);

    }

}
