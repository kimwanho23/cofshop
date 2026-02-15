package kwh.cofshop.coupon.application.port.out;

public interface CouponIssueQueue {
    void enqueueLimitedIssue(Long memberId, Long couponId);
}
