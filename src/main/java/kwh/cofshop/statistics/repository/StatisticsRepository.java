package kwh.cofshop.statistics.repository;

import kwh.cofshop.statistics.dto.DailySalesResponseDto;
import kwh.cofshop.statistics.dto.TopItemResponseDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface StatisticsRepository {

    List<DailySalesResponseDto> getDailySales(LocalDate date); // 일일 판매량

    List<TopItemResponseDto> getTopItemsLast7Days(LocalDateTime time); // 최근 7일 간 판매량

    List<DailySalesResponseDto> getDailySalesBetween(LocalDate start, LocalDate end); // 기간 별 판매량

}
