package kwh.cofshop.coupon.service;

import kwh.cofshop.coupon.api.CouponApi;
import kwh.cofshop.coupon.api.CouponApplyResult;
import kwh.cofshop.coupon.service.factory.CouponDiscountPolicyFactory;
import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class CouponApiAdapter implements CouponApi {

    private final MemberCouponService memberCouponService;
    private final CouponDiscountPolicyFactory couponDiscountPolicyFactory;

    @Override
    public CouponApplyResult applyCoupon(Long memberCouponId, Long memberId, long orderTotalPrice) {
        MemberCoupon memberCoupon = memberCouponService.findValidCoupon(memberCouponId, memberId);
        Coupon coupon = memberCoupon.getCoupon();

        Integer minOrderPrice = coupon.getMinOrderPrice();
        if (minOrderPrice != null && orderTotalPrice < minOrderPrice) {
            throw new BusinessException(BusinessErrorCode.COUPON_NOT_AVAILABLE);
        }

        long discount = couponDiscountPolicyFactory
                .getPolicy(coupon.getType())
                .calculateDiscount(orderTotalPrice, coupon);

        memberCoupon.useCoupon();
        return new CouponApplyResult(memberCoupon.getId(), discount);
    }

    @Override
    public void restoreCoupon(Long memberCouponId) {
        memberCouponService.restoreCoupon(memberCouponId);
    }
}
