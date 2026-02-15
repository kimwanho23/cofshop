package kwh.cofshop.coupon.api;

public record CouponApplyResult(
        Long memberCouponId,
        long discountAmount
) {
}
