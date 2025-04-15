package kwh.cofshop.statistics.dto;

import java.time.LocalDateTime;

public record TopItemDto(
        Long itemId,
        String itemName,
        int totalSold,
        int totalRevenue,
        LocalDateTime lastOrderedAt
) {

}
