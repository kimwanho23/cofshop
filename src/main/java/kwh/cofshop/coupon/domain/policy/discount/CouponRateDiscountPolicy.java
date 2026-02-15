package kwh.cofshop.coupon.domain.policy.discount;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponType;
import org.springframework.stereotype.Component;

@Component
public class CouponRateDiscountPolicy implements CouponDiscountPolicy {

    @Override
    public boolean supports(CouponType type) {
        return type == CouponType.RATE;
    }

    @Override
    public Long calculateDiscount(Long orderPrice, Coupon coupon) {
        long calculatedDiscount = orderPrice * coupon.getDiscountValue() / 100;
        Integer maxDiscount = coupon.getMaxDiscount();
        long boundedByMax = maxDiscount != null ? Math.min(calculatedDiscount, maxDiscount) : calculatedDiscount;
        return Math.min(orderPrice, boundedByMax);
    }
}
