package kwh.cofshop.coupon.service;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.coupon.repository.CouponRepository;
import kwh.cofshop.coupon.repository.MemberCouponRepository;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberCouponIssueServiceTest {

    @Mock
    private MemberCouponRepository memberCouponRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberCouponIssueService memberCouponIssueService;

    @Test
    @DisplayName("쿠폰 발급: 회원 없음")
    void issueCoupon_memberNotFound() {
        when(memberRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberCouponIssueService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("쿠폰 발급: 쿠폰 없음")
    void issueCoupon_couponNotFound() {
        Member member = createMember(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(couponRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberCouponIssueService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("쿠폰 발급: 성공")
    void issueCoupon_success() {
        Member member = createMember(1L);
        Coupon coupon = Coupon.builder().build();
        MemberCoupon saved = MemberCoupon.builder().build();
        ReflectionTestUtils.setField(saved, "id", 100L);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.save(org.mockito.ArgumentMatchers.any(MemberCoupon.class))).thenReturn(saved);

        Long result = memberCouponIssueService.issueCoupon(1L, 1L);

        assertThat(result).isEqualTo(100L);
    }

    private Member createMember(Long id) {
        return Member.builder()
                .id(id)
                .email("user" + id + "@example.com")
                .memberName("사용자")
                .memberPwd("pw")
                .tel("01012341234")
                .build();
    }
}