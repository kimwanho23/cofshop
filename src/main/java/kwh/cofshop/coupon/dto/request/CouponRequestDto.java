package kwh.cofshop.coupon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import kwh.cofshop.coupon.domain.CouponType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CouponRequestDto {

    @NotBlank
    private String name;

    @NotNull
    private CouponType type; // FIXED or PERCENTAGE

    @NotNull
    @Positive
    private Integer discountValue; // 할인 금액

    @PositiveOrZero
    private Integer maxDiscountAmount; // 최대 할인 금액

    @PositiveOrZero
    private Integer minOrderPrice; // 최소 금액 (null이면 자유롭게 사용)

    @Positive
    private Integer couponCount;

    @NotNull
    private LocalDate validFrom;

    @NotNull
    private LocalDate validTo;
}
