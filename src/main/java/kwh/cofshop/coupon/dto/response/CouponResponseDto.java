package kwh.cofshop.coupon.dto.response;

import kwh.cofshop.coupon.domain.CouponType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class CouponResponseDto {
    private Long id;
    private String name;
    private CouponType type;
    private int discountValue;
    private Integer maxDiscountAmount;
    private Integer minOrderPrice;
    private LocalDate validFrom;
    private LocalDate validTo;
    private LocalDateTime createdAt;
}
