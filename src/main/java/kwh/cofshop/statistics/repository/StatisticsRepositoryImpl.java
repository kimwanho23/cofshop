package kwh.cofshop.statistics.repository;

import kwh.cofshop.order.api.OrderDailySales;
import kwh.cofshop.order.api.OrderStatisticsPort;
import kwh.cofshop.order.api.OrderTopItemSales;
import kwh.cofshop.statistics.dto.DailySalesResponseDto;
import kwh.cofshop.statistics.dto.TopItemResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class StatisticsRepositoryImpl implements StatisticsRepository {

    private final OrderStatisticsPort orderStatisticsPort;

    @Override
    public List<DailySalesResponseDto> getDailySales(LocalDate date) {
        return orderStatisticsPort.getDailySales(date).stream()
                .map(this::toDailySalesResponse)
                .toList();
    }

    @Override
    public List<TopItemResponseDto> getTopItemsLast7Days(LocalDateTime time) {
        return orderStatisticsPort.getTopItemsLast7Days(time).stream()
                .map(this::toTopItemResponse)
                .toList();
    }

    @Override
    public List<DailySalesResponseDto> getDailySalesBetween(LocalDate start, LocalDate end) {
        return orderStatisticsPort.getDailySalesBetween(start, end).stream()
                .map(this::toDailySalesResponse)
                .toList();
    }

    private DailySalesResponseDto toDailySalesResponse(OrderDailySales source) {
        return new DailySalesResponseDto(
                source.date(),
                source.totalSales(),
                source.totalRevenue()
        );
    }

    private TopItemResponseDto toTopItemResponse(OrderTopItemSales source) {
        return new TopItemResponseDto(
                source.itemId(),
                source.itemName(),
                source.totalSold(),
                source.totalRevenue(),
                source.lastOrderDate()
        );
    }
}