package kwh.cofshop.coupon.api;

public interface CouponApi {

    CouponApplyResult applyCoupon(Long memberCouponId, Long memberId, long orderTotalPrice);

    void restoreCoupon(Long memberCouponId);
}
