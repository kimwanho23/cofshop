package kwh.cofshop.order.api;

import java.time.LocalDateTime;

public record OrderTopItemSales(
        Long itemId,
        String itemName,
        Integer totalSold,
        Integer totalRevenue,
        LocalDateTime lastOrderDate
) {
}
