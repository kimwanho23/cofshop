package kwh.cofshop.coupon.service;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.domain.CouponType;
import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.coupon.dto.response.MemberCouponResponseDto;
import kwh.cofshop.coupon.factory.CouponIssuePolicyFactory;
import kwh.cofshop.coupon.mapper.MemberCouponMapper;
import kwh.cofshop.coupon.policy.issue.CouponIssuePolicy;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberCouponServiceTest {

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
    @DisplayName("쿠폰 발급: 회원 없음")
    void issueCoupon_memberNotFound() {
        when(memberRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberCouponService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("쿠폰 발급: 쿠폰 없음")
    void issueCoupon_couponNotFound() {
        Member member = createMember(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(couponRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberCouponService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("쿠폰 발급: 유효기간 아님")
    void issueCoupon_invalidDate() {
        Member member = createMember(1L);
        Coupon coupon = createCoupon(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2));

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> memberCouponService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("쿠폰 발급: 성공")
    void issueCoupon_success() {
        Member member = createMember(1L);
        Coupon coupon = createCoupon(LocalDate.now().minusDays(1), LocalDate.now().plusDays(5));
        CouponIssuePolicy policy = org.mockito.Mockito.mock(CouponIssuePolicy.class);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(couponIssuePolicyFactory.getPolicy(coupon)).thenReturn(policy);

        memberCouponService.issueCoupon(1L, 1L);

        verify(policy).issue(member, coupon);
    }

    @Test
    @DisplayName("내 쿠폰 목록")
    void memberCouponList() {
        MemberCoupon memberCoupon = MemberCoupon.builder().build();
        when(memberCouponRepository.findByMemberId(1L)).thenReturn(List.of(memberCoupon));
        when(memberCouponMapper.toResponseDto(memberCoupon)).thenReturn(new MemberCouponResponseDto());

        List<MemberCouponResponseDto> results = memberCouponService.memberCouponList(1L);

        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("유효 쿠폰 조회: 대상 없음")
    void findValidCoupon_notFound() {
        when(memberCouponRepository.findValidCouponById(anyLong(), anyLong(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberCouponService.findValidCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("유효 쿠폰 조회")
    void findValidCoupon_success() {
        MemberCoupon memberCoupon = MemberCoupon.builder().state(CouponState.AVAILABLE).build();
        when(memberCouponRepository.findValidCouponById(anyLong(), anyLong(), any()))
                .thenReturn(Optional.of(memberCoupon));

        MemberCoupon result = memberCouponService.findValidCoupon(1L, 1L);

        assertThat(result).isSameAs(memberCoupon);
    }

    @Test
    @DisplayName("유효 쿠폰 응답 조회")
    void findValidCouponResponse() {
        MemberCoupon memberCoupon = MemberCoupon.builder().state(CouponState.AVAILABLE).build();
        MemberCouponResponseDto responseDto = new MemberCouponResponseDto();

        when(memberCouponRepository.findValidCouponById(anyLong(), anyLong(), any()))
                .thenReturn(Optional.of(memberCoupon));
        when(memberCouponMapper.toResponseDto(memberCoupon)).thenReturn(responseDto);

        MemberCouponResponseDto result = memberCouponService.findValidCouponResponse(1L, 1L);

        assertThat(result).isSameAs(responseDto);
    }

    @Test
    @DisplayName("쿠폰 복구: 성공")
    void restoreCoupon_success() {
        MemberCoupon memberCoupon = MemberCoupon.builder().state(CouponState.USED).build();
        when(memberCouponRepository.findById(1L)).thenReturn(Optional.of(memberCoupon));

        memberCouponService.restoreCoupon(1L);

        assertThat(memberCoupon.getState()).isEqualTo(CouponState.AVAILABLE);
    }

    @Test
    @DisplayName("쿠폰 복구: 상태 오류")
    void restoreCoupon_invalidState() {
        MemberCoupon memberCoupon = MemberCoupon.builder().state(CouponState.AVAILABLE).build();
        when(memberCouponRepository.findById(1L)).thenReturn(Optional.of(memberCoupon));

        assertThatThrownBy(() -> memberCouponService.restoreCoupon(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("쿠폰 만료 처리")
    void expireMemberCoupons() {
        MemberCoupon coupon1 = MemberCoupon.builder().state(CouponState.AVAILABLE).build();
        MemberCoupon coupon2 = MemberCoupon.builder().state(CouponState.AVAILABLE).build();

        when(memberCouponRepository.findByCouponExpired(any(), any()))
                .thenReturn(List.of(coupon1, coupon2));

        int result = memberCouponService.expireMemberCoupons(LocalDate.now());

        assertThat(result).isEqualTo(2);
        assertThat(coupon1.getState()).isEqualTo(CouponState.EXPIRED);
        assertThat(coupon2.getState()).isEqualTo(CouponState.EXPIRED);
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

    private Coupon createCoupon(LocalDate validFrom, LocalDate validTo) {
        return Coupon.builder()
                .name("테스트 쿠폰")
                .type(CouponType.FIXED)
                .discountValue(1000)
                .state(CouponState.AVAILABLE)
                .validFrom(validFrom)
                .validTo(validTo)
                .build();
    }
}