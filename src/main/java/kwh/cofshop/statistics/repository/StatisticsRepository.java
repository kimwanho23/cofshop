package kwh.cofshop.statistics.repository;

import kwh.cofshop.order.domain.Order;
import kwh.cofshop.statistics.dto.DailySalesDto;
import kwh.cofshop.statistics.dto.TopItemDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface StatisticsRepository {

    List<DailySalesDto> getDailySales(LocalDate date); // 일일 판매량
    List<TopItemDto> getTopItemsLast7Days(LocalDateTime time); // 최근 7일 간 판매량
    List<DailySalesDto> getDailySalesBetween(LocalDate start, LocalDate end); // 기간 별 판매량

}
