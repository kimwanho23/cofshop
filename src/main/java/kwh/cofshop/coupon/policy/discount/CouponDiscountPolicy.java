package kwh.cofshop.coupon.policy.discount;

import kwh.cofshop.coupon.domain.CouponType;

public interface CouponDiscountPolicy {

    boolean supports(CouponType type);

    Long calculateDiscount(Long value);
}
