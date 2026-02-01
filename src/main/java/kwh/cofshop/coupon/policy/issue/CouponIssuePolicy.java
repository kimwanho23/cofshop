package kwh.cofshop.coupon.policy.issue;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.member.domain.Member;

// 수량이 존재하는 쿠폰과 무제한인 쿠폰의 발급 정책 인터페이스
public interface CouponIssuePolicy {
    boolean supports(Coupon coupon);

    void issue(Member member, Coupon coupon);
}
