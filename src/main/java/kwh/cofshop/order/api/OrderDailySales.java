package kwh.cofshop.order.api;

import java.time.LocalDate;

public record OrderDailySales(
        LocalDate date,
        Long totalSales,
        Long totalRevenue
) {
}
