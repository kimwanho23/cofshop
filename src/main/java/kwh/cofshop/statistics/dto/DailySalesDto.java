package kwh.cofshop.statistics.dto;

import java.time.LocalDate;

public record DailySalesDto(LocalDate date,
                            Long totalSales,
                            Long totalRevenue) {
}
