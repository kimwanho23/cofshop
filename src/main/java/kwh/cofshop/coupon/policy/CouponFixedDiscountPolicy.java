package kwh.cofshop.coupon.policy;


public class CouponFixedDiscountPolicy implements CouponDiscountPolicy {

    private final int discountValue;

    public CouponFixedDiscountPolicy(int discountValue) {
        this.discountValue = discountValue;
    }


    @Override
    public Long calculateDiscount(Long value) {
        return Math.min(value, discountValue);
    }

}
