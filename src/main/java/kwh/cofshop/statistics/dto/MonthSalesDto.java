package kwh.cofshop.statistics.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonthSalesDto {
    private Integer year;
    private Integer month;
    private Long orderCount;
    private Integer totalPrice;
    private Integer totalCouponDiscount;
    private Integer totalPointUsed;
    private Integer totalFinalPrice;
}
