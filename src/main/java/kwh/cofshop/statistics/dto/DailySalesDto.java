package kwh.cofshop.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DailySalesDto {
    private LocalDate date;
    private Long totalSales;
    private Long totalRevenue;
}
