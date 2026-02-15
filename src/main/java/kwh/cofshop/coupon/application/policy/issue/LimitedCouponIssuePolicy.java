package kwh.cofshop.coupon.application.policy.issue;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.policy.issue.CouponIssuePolicy;
import kwh.cofshop.coupon.application.port.out.CouponIssueQueue;
import kwh.cofshop.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LimitedCouponIssuePolicy implements CouponIssuePolicy {

    private final CouponIssueQueue couponIssueQueue;

    @Override
    public boolean supports(Coupon coupon) {
        return coupon.isLimitedQuantity();
    }

    @Override
    public void issue(Member member, Coupon coupon) {
        couponIssueQueue.enqueueLimitedIssue(member.getId(), coupon.getId());
    }
}
