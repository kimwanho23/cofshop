package kwh.cofshop.statistics.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.DateTemplate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.item.domain.QItem;
import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.domain.QOrder;
import kwh.cofshop.order.domain.QOrderItem;
import kwh.cofshop.statistics.dto.DailySalesDto;
import kwh.cofshop.statistics.dto.TopItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class StatisticsRepositoryImpl implements StatisticsRepository {

    private final JPAQueryFactory queryFactory;

    QOrder order = QOrder.order;
    QOrderItem orderItem = QOrderItem.orderItem;
    QItem item = QItem.item;

    DateTemplate<LocalDate> dateOnly = Expressions.dateTemplate( //LocalDateTime to LocalDate
            LocalDate.class,
            "DATE({0})",
            order.orderDate
    );

    // 일별 판매량
    @Override
    public List<DailySalesDto> getDailySales(LocalDate date) {

        return queryFactory
                .select(Projections.constructor(
                        DailySalesDto.class,
                        dateOnly,
                        orderItem.orderPrice.multiply(orderItem.quantity).sum()
                ))
                .from(order)
                .join(order.orderItems, orderItem)
                .where(order.orderState.eq(OrderState.COMPLETED)
                        .and(dateOnly.eq(date)))
                .groupBy(dateOnly)
                .fetch();
    }

    // 최근 일주일 간 TOP 5 품목
    @Override
    public List<TopItemDto> getTopItemsLast7Days(LocalDateTime time) {

        NumberExpression<Integer> totalSold = orderItem.quantity.sum(); // 판매량
        NumberExpression<Integer> totalRevenue = orderItem.orderPrice.multiply(orderItem.quantity).sum(); // 매출

        return queryFactory.select(Projections.constructor(
                        TopItemDto.class,
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
                .orderBy(
                        totalSold.desc(), // 판매량 순
                        totalRevenue.desc() // 판매량이 동일하면 매출 순
                )
                .limit(5)
                .fetch();
    }

    @Override
    public List<DailySalesDto> getDailySalesBetween(LocalDate start, LocalDate end) {

        NumberExpression<Integer> totalSales = orderItem.quantity.sum(); // 판매량
        NumberExpression<Integer> totalRevenue = orderItem.orderPrice.multiply(orderItem.quantity).sum(); // 매출

        return queryFactory
                .select(Projections.constructor(
                        DailySalesDto.class,
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
}
