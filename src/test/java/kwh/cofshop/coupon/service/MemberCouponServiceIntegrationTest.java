package kwh.cofshop.coupon.service;

import kwh.cofshop.coupon.factory.CouponIssuePolicyFactory;
import kwh.cofshop.coupon.mapper.MemberCouponMapper;
import kwh.cofshop.coupon.repository.CouponRepository;
import kwh.cofshop.coupon.repository.MemberCouponRepository;
import kwh.cofshop.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MemberCouponServiceIntegrationTest {

    @Mock
    private MemberCouponRepository memberCouponRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberCouponRedisService memberCouponRedisService;

    @Mock
    private MemberCouponMapper memberCouponMapper;

    @Mock
    private CouponIssuePolicyFactory couponIssuePolicyFactory;

    @InjectMocks
    private MemberCouponService memberCouponService;

    @Test
    @DisplayName("서비스 생성")
    void createService() {
        assertThat(memberCouponService).isNotNull();
    }
}
