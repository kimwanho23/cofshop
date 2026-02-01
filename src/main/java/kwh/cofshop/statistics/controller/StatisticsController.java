package kwh.cofshop.statistics.controller;

import io.swagger.v3.oas.annotations.Operation;
import kwh.cofshop.statistics.dto.DailySalesDto;
import kwh.cofshop.statistics.dto.TopItemDto;
import kwh.cofshop.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "하루 상품 판매량 조회", description = "하루의 판매량을 조회합니다.")
    @GetMapping("/daily-sales")
    public List<DailySalesDto> getDailySales(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<DailySalesDto> dailySales = statisticsService.getDailySales(date);
        return dailySales;
    }

    @Operation(summary = "최근 7일 간 인기상품 조회", description = "최근 7일 동안 많이 팔린 상품을 10개 조회합니다.")
    @GetMapping("/last-7days")
    public List<TopItemDto> getTopItemsLast7Days() {
        List<TopItemDto> last7Days = statisticsService.getTopItemsLast7Days();
        return last7Days;
    }

    @Operation(summary = "기간 별 판매량 순으로 상품 조회", description = "기간 별로 팔린 상품들의 정보를 조회합니다.")
    @GetMapping("/sales-between")
    public List<DailySalesDto> getSalesBetween(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        List<DailySalesDto> result = statisticsService.getDailySalesBetween(start, end);
        return result;
    }


}
