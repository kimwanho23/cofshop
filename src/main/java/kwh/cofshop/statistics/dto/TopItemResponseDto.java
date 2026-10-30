package kwh.cofshop.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TopItemResponseDto {
    private Long itemId;
    private String itemName;
    private Long totalSold;
    private Long totalRevenue;
    private LocalDateTime lastOrderDate;
}
