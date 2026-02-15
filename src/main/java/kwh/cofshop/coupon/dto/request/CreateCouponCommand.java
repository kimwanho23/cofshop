package kwh.cofshop.coupon.dto.request;

import kwh.cofshop.coupon.domain.CouponType;

import java.time.LocalDate;

public record CreateCouponCommand(
        String name,
        Integer minOrderPrice,
        Integer discountValue,
        Integer maxDiscountAmount,
        CouponType type,
        Integer couponCount,
        LocalDate validFrom,
        LocalDate validTo
) {
}
