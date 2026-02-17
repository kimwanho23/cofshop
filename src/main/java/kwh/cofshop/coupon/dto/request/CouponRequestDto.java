package kwh.cofshop.coupon.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import kwh.cofshop.coupon.domain.CouponType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CouponRequestDto {

    @NotBlank
    @Size(max = 100, message = "Coupon name must be at most 100 characters.")
    private String name;

    @NotNull
    private CouponType type;

    @NotNull
    @Positive
    private Integer discountValue;

    @PositiveOrZero
    private Integer maxDiscountAmount;

    @PositiveOrZero
    private Integer minOrderPrice;

    @Positive
    private Integer couponCount;

    @NotNull
    private LocalDate validFrom;

    @NotNull
    private LocalDate validTo;

    @AssertTrue(message = "Coupon validFrom must be on or before validTo.")
    private boolean isValidPeriod() {
        if (validFrom == null || validTo == null) {
            return true;
        }
        return !validFrom.isAfter(validTo);
    }
}
