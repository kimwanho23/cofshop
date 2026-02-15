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
    @Size(max = 100, message = "ì¿ í°ëª…ì? 100???´í•˜?¬ì•¼ ?©ë‹ˆ??")
    private String name;

    @NotNull
    private CouponType type; // FIXED or PERCENTAGE

    @NotNull
    @Positive
    private Integer discountValue; // ? ì¸ ê¸ˆì•¡

    @PositiveOrZero
    private Integer maxDiscountAmount; // ìµœë? ? ì¸ ê¸ˆì•¡

    @PositiveOrZero
    private Integer minOrderPrice; // ìµœì†Œ ê¸ˆì•¡ (null?´ë©´ ?ìœ ë¡?²Œ ?¬ìš©)

    @Positive
    private Integer couponCount;

    @NotNull
    private LocalDate validFrom;

    @NotNull
    private LocalDate validTo;

    @AssertTrue(message = "ì¿ í° ?œì‘?¼ì? ì¢…ë£Œ?¼ë³´????„ ???†ìŠµ?ˆë‹¤.")
    private boolean isValidPeriod() {
        if (validFrom == null || validTo == null) {
            return true;
        }
        return !validFrom.isAfter(validTo);
    }
}
