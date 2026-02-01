package kwh.cofshop.coupon.policy.issue;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.service.MemberCouponIssueService;
import kwh.cofshop.member.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UnlimitedCouponIssuePolicy implements CouponIssuePolicy {

    private final MemberCouponIssueService memberCouponIssueService;

    @Override
    public boolean supports(Coupon coupon) {
        return !coupon.isLimitedQuantity();
    }

    @Override
    public void issue(Member member, Coupon coupon) {
        memberCouponIssueService.issueCoupon(member.getId(), coupon.getId());
    }
}
