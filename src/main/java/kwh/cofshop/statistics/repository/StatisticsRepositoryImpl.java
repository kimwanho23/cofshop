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
import kwh.cofshop.statistics.dto.DailySalesResponseDto;
import kwh.cofshop.statistics.dto.TopItemResponseDto;
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
    public List<DailySalesResponseDto> getDailySales(LocalDate date) {
        NumberExpression<Long> totalSales = orderItem.quantity.sum().longValue();
        NumberExpression<Long> totalRevenue = discountedUnitPrice().multiply(orderItem.quantity).sum().longValue();

        return queryFactory
                .select(Projections.constructor(
                        DailySalesResponseDto.class,
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

    // 최근 일주일 간 TOP 5 품목
    @Override
    public List<TopItemResponseDto> getTopItemsLast7Days(LocalDateTime time) {

        NumberExpression<Integer> totalSold = orderItem.quantity.sum(); // 판매량
        NumberExpression<Integer> totalRevenue = discountedUnitPrice().multiply(orderItem.quantity).sum(); // 매출

        return queryFactory.select(Projections.constructor(
                        TopItemResponseDto.class,
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
    public List<DailySalesResponseDto> getDailySalesBetween(LocalDate start, LocalDate end) {

        NumberExpression<Long> totalSales = orderItem.quantity.sum().longValue(); // 판매량
        NumberExpression<Long> totalRevenue = discountedUnitPrice().multiply(orderItem.quantity).sum().longValue(); // 매출

        return queryFactory
                .select(Projections.constructor(
                        DailySalesResponseDto.class,
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
