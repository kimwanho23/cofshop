package kwh.cofshop.coupon.domain.policy.discount;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponType;
import org.springframework.stereotype.Component;

@Component
public class CouponFixedDiscountPolicy implements CouponDiscountPolicy {

    @Override
    public boolean supports(CouponType type) {
        return type == CouponType.FIXED;
    }

    @Override
    public Long calculateDiscount(Long orderPrice, Coupon coupon) {
        return Math.min(orderPrice, coupon.getDiscountValue());
    }

}
