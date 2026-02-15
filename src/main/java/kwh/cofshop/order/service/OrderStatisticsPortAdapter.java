package kwh.cofshop.order.service;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.DateTemplate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.item.domain.QItem;
import kwh.cofshop.order.api.OrderDailySales;
import kwh.cofshop.order.api.OrderStatisticsPort;
import kwh.cofshop.order.api.OrderTopItemSales;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.domain.QOrder;
import kwh.cofshop.order.domain.QOrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderStatisticsPortAdapter implements OrderStatisticsPort {

    private final JPAQueryFactory queryFactory;

    private final QOrder order = QOrder.order;
    private final QOrderItem orderItem = QOrderItem.orderItem;
    private final QItem item = QItem.item;

    private final DateTemplate<LocalDate> dateOnly = Expressions.dateTemplate(
            LocalDate.class,
            "DATE({0})",
            order.orderDate
    );

    @Override
    public List<OrderDailySales> getDailySales(LocalDate date) {
        NumberExpression<Long> totalSales = orderItem.quantity.sum().longValue();
        NumberExpression<Long> totalRevenue = discountedUnitPrice().multiply(orderItem.quantity).sum().longValue();

        return queryFactory
                .select(Projections.constructor(
                        OrderDailySales.class,
                        dateOnly,
                        totalSales,
                        totalRevenue
                ))
                .from(order)
                .join(order.orderItems, orderItem)
                .where(order.orderState.eq(OrderState.COMPLETED)
                        .and(dateOnly.eq(date)))
                .groupBy(dateOnly)
                .fetch();
    }

    @Override
    public List<OrderTopItemSales> getTopItemsLast7Days(LocalDateTime time) {
        NumberExpression<Integer> totalSold = orderItem.quantity.sum();
        NumberExpression<Integer> totalRevenue = discountedUnitPrice().multiply(orderItem.quantity).sum();

        return queryFactory.select(Projections.constructor(
                        OrderTopItemSales.class,
                        item.id,
                        item.itemName,
                        totalSold,
                        totalRevenue,
                        order.orderDate.max()
                ))
                .from(orderItem)
                .join(orderItem.item, item)
                .join(orderItem.order, order)
                .where(order.orderState.eq(OrderState.COMPLETED)
                        .and(order.orderDate.goe(time)))
                .groupBy(item.id, item.itemName)
                .orderBy(totalSold.desc(), totalRevenue.desc())
                .limit(5)
                .fetch();
    }

    @Override
    public List<OrderDailySales> getDailySalesBetween(LocalDate start, LocalDate end) {
        NumberExpression<Long> totalSales = orderItem.quantity.sum().longValue();
        NumberExpression<Long> totalRevenue = discountedUnitPrice().multiply(orderItem.quantity).sum().longValue();

        return queryFactory
                .select(Projections.constructor(
                        OrderDailySales.class,
                        dateOnly,
                        totalSales,
                        totalRevenue
                ))
                .from(order)
                .join(order.orderItems, orderItem)
                .where(order.orderState.eq(OrderState.COMPLETED)
                        .and(order.orderDate.between(start.atStartOfDay(), end.atTime(LocalTime.MAX))))
                .groupBy(dateOnly)
                .orderBy(dateOnly.asc())
                .fetch();
    }

    private NumberExpression<Integer> discountedUnitPrice() {
        return Expressions.numberTemplate(
                Integer.class,
                "({0} * (100 - {1}) / 100)",
                orderItem.orderPrice,
                orderItem.discountRate
        );
    }
}
