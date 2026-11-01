package kwh.cofshop.statistics.controller;

import io.swagger.v3.oas.annotations.Operation;
import kwh.cofshop.statistics.dto.DailySalesResponseDto;
import kwh.cofshop.statistics.dto.TopItemResponseDto;
import kwh.cofshop.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/daily-sales")
    public List<DailySalesResponseDto> getDailySales(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<DailySalesResponseDto> dailySales = statisticsService.getDailySales(date);
        return dailySales;
    }

    @Operation(summary = "최근 7일 간 인기상품 조회", description = "최근 7일 동안 많이 팔린 상품과 매출을 10개 조회합니다. 고객용 인기 상품은 /api/item/populars를 사용합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/last-7days")
    public List<TopItemResponseDto> getTopItemsLast7Days() {
        List<TopItemResponseDto> last7Days = statisticsService.getTopItemsLast7Days();
        return last7Days;
    }

    @Operation(summary = "기간 별 판매량 순으로 상품 조회", description = "기간 별로 팔린 상품들의 정보를 조회합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/sales-between")
    public List<DailySalesResponseDto> getSalesBetween(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        List<DailySalesResponseDto> result = statisticsService.getDailySalesBetween(start, end);
        return result;
    }


}
