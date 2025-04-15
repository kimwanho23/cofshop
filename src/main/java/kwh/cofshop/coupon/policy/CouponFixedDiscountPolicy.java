package kwh.cofshop.coupon.policy;


public class CouponFixedDiscountPolicy implements CouponDiscountPolicy {

    private final int discountValue;

    public CouponFixedDiscountPolicy(int discountValue) {
        this.discountValue = discountValue;
    }

    @Override
    public int calculateDiscount(int price) {
        return Math.min(price, discountValue);
    }
}
