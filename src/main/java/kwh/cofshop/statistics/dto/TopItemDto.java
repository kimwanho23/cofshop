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
public class TopItemDto {
    private Long itemId;
    private String itemName;
    private Integer totalSold;
    private Integer totalRevenue;
    private LocalDateTime lastOrderDate;
}