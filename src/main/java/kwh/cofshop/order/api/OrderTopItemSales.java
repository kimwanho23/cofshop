package kwh.cofshop.order.api;

import java.time.LocalDateTime;

public record OrderTopItemSales(
        Long itemId,
        String itemName,
        Long totalSold,
        Long totalRevenue,
        LocalDateTime lastOrderDate
) {
}
