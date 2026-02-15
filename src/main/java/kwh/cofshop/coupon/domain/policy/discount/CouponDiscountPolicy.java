package kwh.cofshop.coupon.domain.policy.discount;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponType;

public interface CouponDiscountPolicy {

    boolean supports(CouponType type);

    Long calculateDiscount(Long orderPrice, Coupon coupon);
}
