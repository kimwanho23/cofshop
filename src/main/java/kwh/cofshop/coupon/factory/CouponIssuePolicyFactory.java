package kwh.cofshop.coupon.factory;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.policy.issue.CouponIssuePolicy;
import kwh.cofshop.coupon.policy.issue.LimitedCouponIssuePolicy;
import kwh.cofshop.coupon.policy.issue.UnlimitedCouponIssuePolicy;
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
