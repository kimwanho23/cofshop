package kwh.cofshop.coupon.presentation.dto.request;

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
    @Size(max = 100, message = "쿠폰명은 100자 이하여야 합니다.")
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

    @AssertTrue(message = "쿠폰 시작일은 종료일보다 늦을 수 없습니다.")
    private boolean isValidPeriod() {
        if (validFrom == null || validTo == null) {
            return true;
        }
        return !validFrom.isAfter(validTo);
    }
}
