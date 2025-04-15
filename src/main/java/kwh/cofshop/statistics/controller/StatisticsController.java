package kwh.cofshop.statistics.controller;

import kwh.cofshop.global.response.ApiResponse;
import kwh.cofshop.statistics.dto.DailySalesDto;
import kwh.cofshop.statistics.dto.TopItemDto;
import kwh.cofshop.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/daily-sales")
    public ResponseEntity<ApiResponse<List<DailySalesDto>>> getDailySales(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<DailySalesDto> dailySales = statisticsService.getDailySales(date);
        return ResponseEntity.ok(ApiResponse.OK(dailySales));
    }

    @GetMapping("/last-7days")
    public ResponseEntity<ApiResponse<List<TopItemDto>>> getTopItemsLast7Days() {
        List<TopItemDto> last7Days = statisticsService.getTopItemsLast7Days();
        return ResponseEntity.ok(ApiResponse.OK(last7Days));
    }

    @GetMapping("/sales-between")
    public ResponseEntity<ApiResponse<List<DailySalesDto>>> getSalesBetween(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        List<DailySalesDto> result = statisticsService.getDailySalesBetween(start, end);
        return ResponseEntity.ok(ApiResponse.OK(result));
    }



}
