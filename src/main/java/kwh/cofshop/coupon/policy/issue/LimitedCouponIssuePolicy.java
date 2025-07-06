package kwh.cofshop.coupon.policy.issue;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.service.MemberCouponRedisService;
import kwh.cofshop.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LimitedCouponIssuePolicy implements CouponIssuePolicy {

    private final MemberCouponRedisService memberCouponRedisService;

    @Override
    public boolean supports(Coupon coupon) {
        return coupon.isLimitedQuantity();
    }

    @Override
    public void issue(Member member, Coupon coupon) {
        memberCouponRedisService.enqueueLimitedCouponIssueRequest(member.getId(), coupon.getId());
    }
}
