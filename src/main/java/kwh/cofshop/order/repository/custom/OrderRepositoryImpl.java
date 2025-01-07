package kwh.cofshop.order.repository.custom;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.item.domain.QItem;
import kwh.cofshop.item.domain.QItemOption;
import kwh.cofshop.member.domain.QMember;
import kwh.cofshop.member.dto.MemberResponseDto;
import kwh.cofshop.order.domain.QOrder;
import kwh.cofshop.order.domain.QOrderItem;
import kwh.cofshop.order.dto.request.OrdererRequestDto;
import kwh.cofshop.order.dto.response.OrderItemResponseDto;
import kwh.cofshop.order.dto.response.OrderResponseDto;
import kwh.cofshop.order.dto.response.OrdererResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    @Override
    public OrderResponseDto findByOrderIdWithItems(Long orderId) {
        QOrder order = QOrder.order;
        QMember member = QMember.member;
        QOrderItem orderItem = QOrderItem.orderItem;
        QItemOption itemOption = QItemOption.itemOption;
        QItem item = QItem.item;

        // Member
        OrdererResponseDto orderer = queryFactory
                .select(Projections.constructor(OrdererResponseDto.class,
                        member.email,
                        order.address  // Address VO 직접 전달
                ))
                .from(order)
                .join(order.member, member)
                .where(order.orderId.eq(orderId))
                .fetchOne();

        // OrderItem
        List<OrderItemResponseDto> orderItems = queryFactory
                .select(Projections.constructor(OrderItemResponseDto.class,
                        item.itemId,
                        itemOption.optionId,
                        orderItem.orderPrice,
                        orderItem.quantity
                ))
                .from(orderItem)
                .join(orderItem.item, item)
                .join(orderItem.itemOption, itemOption)
                .where(orderItem.order.orderId.eq(orderId))
                .fetch();

        OrderResponseDto orderResponseDto = new OrderResponseDto();
        orderResponseDto.setOrdererResponseDto(orderer);
        orderResponseDto.setOrderItemResponseDto(orderItems);

        return orderResponseDto;
    }
}
