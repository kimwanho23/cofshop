package kwh.cofshop.coupon.service.factory;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.policy.issue.CouponIssuePolicy;
import kwh.cofshop.coupon.service.policy.issue.LimitedCouponIssuePolicy;
import kwh.cofshop.coupon.service.policy.issue.UnlimitedCouponIssuePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponIssuePolicyFactory {

    private final LimitedCouponIssuePolicy limitedPolicy;
    private final UnlimitedCouponIssuePolicy unlimitedPolicy;

    public CouponIssuePolicy getPolicy(Coupon coupon) {
        return coupon.isLimitedQuantity() ? limitedPolicy : unlimitedPolicy;
    }
}
