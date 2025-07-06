package kwh.cofshop.coupon.service;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.factory.CouponIssuePolicyFactory;
import kwh.cofshop.coupon.policy.issue.CouponIssuePolicy;
import kwh.cofshop.coupon.policy.issue.LimitedCouponIssuePolicy;
import kwh.cofshop.coupon.policy.issue.UnlimitedCouponIssuePolicy;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@Slf4j
@ExtendWith(MockitoExtension.class)
class MemberCouponServiceTest{

    @Mock
    private LimitedCouponIssuePolicy limitedPolicy;

    @Mock
    private UnlimitedCouponIssuePolicy unlimitedPolicy;

    @InjectMocks
    private CouponIssuePolicyFactory policyFactory;

    @Test
    @DisplayName("수량 있는 쿠폰 → LimitedPolicy 사용")
    void 수량_제한_쿠폰_발급() {
        // given
        Coupon limitedCoupon = Coupon.builder()
                .couponCount(100) // 수량 있음
                .build();

        // when
        CouponIssuePolicy policy = policyFactory.getPolicy(limitedCoupon);
        assertThat(policy).isInstanceOf(LimitedCouponIssuePolicy.class);
    }

    @Test
    @DisplayName("수량 없는 쿠폰 → UnlimitedPolicy 사용")
    void 수량_무제한_쿠폰_발급() {
        // given
        Coupon unlimitedCoupon = Coupon.builder()
                .couponCount(null) // 수량 없음
                .build();

        // when
        CouponIssuePolicy policy = policyFactory.getPolicy(unlimitedCoupon);
        assertThat(policy).isInstanceOf(UnlimitedCouponIssuePolicy.class);
    }
}