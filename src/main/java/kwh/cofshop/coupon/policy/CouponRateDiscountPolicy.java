package kwh.cofshop.coupon.policy;

public class CouponRateDiscountPolicy implements CouponDiscountPolicy {

    private final int discountRate; // 할인율
    private final Integer maxDiscount; // 최대 할인 금액

    public CouponRateDiscountPolicy(int discountRate, Integer maxDiscount) {
        this.discountRate = discountRate;
        this.maxDiscount = maxDiscount;
    }

    @Override
    public Long calculateDiscount(Long price) {
        long calculatedPrice = price * discountRate / 100;
        return (maxDiscount != null) ? Math.min(calculatedPrice, maxDiscount) : calculatedPrice;
    }
}
