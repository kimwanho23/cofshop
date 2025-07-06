package kwh.cofshop.coupon.factory;

import kwh.cofshop.coupon.domain.CouponType;
import kwh.cofshop.coupon.policy.discount.CouponDiscountPolicy;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CouponDiscountPolicyFactory {

    private final List<CouponDiscountPolicy> policies;

    public CouponDiscountPolicy getPolicy(CouponType type) {
        return policies.stream()
                .filter(policy -> policy.supports(type))
                .findFirst()
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.COUPON_NOT_FOUND));
    }
}
