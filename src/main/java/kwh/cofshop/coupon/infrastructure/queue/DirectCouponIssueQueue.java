package kwh.cofshop.coupon.infrastructure.queue;

import kwh.cofshop.coupon.application.port.out.CouponIssueQueue;
import kwh.cofshop.coupon.application.service.MemberCouponIssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DirectCouponIssueQueue implements CouponIssueQueue {

    private final MemberCouponIssueService memberCouponIssueService;

    @Override
    public void enqueueLimitedIssue(Long memberId, Long couponId) {
        memberCouponIssueService.issueCoupon(memberId, couponId);
    }
}
