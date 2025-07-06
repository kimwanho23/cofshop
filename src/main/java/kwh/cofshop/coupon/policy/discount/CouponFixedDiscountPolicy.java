package kwh.cofshop.coupon.policy.discount;


import kwh.cofshop.coupon.domain.CouponType;

public class CouponFixedDiscountPolicy implements CouponDiscountPolicy {

    private final int discountValue;

    public CouponFixedDiscountPolicy(int discountValue) {
        this.discountValue = discountValue;
    }


    @Override
    public boolean supports(CouponType type) {
        return type == CouponType.FIXED;
    }

    @Override
    public Long calculateDiscount(Long value) {
        return Math.min(value, discountValue);
    }

}
