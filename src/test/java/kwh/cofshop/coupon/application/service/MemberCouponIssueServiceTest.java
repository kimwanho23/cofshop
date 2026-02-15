package kwh.cofshop.coupon.application.service;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.coupon.infrastructure.outbox.service.CouponIssueOutboxService;
import kwh.cofshop.coupon.infrastructure.persistence.repository.CouponRepository;
import kwh.cofshop.coupon.infrastructure.persistence.repository.MemberCouponRepository;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberCouponIssueServiceTest {

    @Mock
    private MemberCouponRepository memberCouponRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CouponIssueOutboxService couponIssueOutboxService;

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
        Coupon coupon = createCoupon(CouponState.AVAILABLE, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        MemberCoupon saved = MemberCoupon.builder().build();
        ReflectionTestUtils.setField(saved, "id", 100L);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByMember_IdAndCoupon_Id(1L, 1L)).thenReturn(false);
        when(memberCouponRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(MemberCoupon.class))).thenReturn(saved);

        Long result = memberCouponIssueService.issueCoupon(1L, 1L);

        assertThat(result).isEqualTo(100L);
        verify(couponIssueOutboxService).enqueueCouponIssued(100L, 1L, 1L);
    }

    @Test
    @DisplayName("쿠폰 발급: 실제 발급 시점에 쿠폰 상태가 사용 불가")
    void issueCoupon_couponNotAvailableState() {
        Member member = createMember(1L);
        Coupon coupon = createCoupon(CouponState.CANCELLED, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> memberCouponIssueService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("쿠폰 발급: 실제 발급 시점에 쿠폰 만료")
    void issueCoupon_couponExpiredAtIssuance() {
        Member member = createMember(1L);
        Coupon coupon = createCoupon(CouponState.AVAILABLE, LocalDate.now().minusDays(10), LocalDate.now().minusDays(1));

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> memberCouponIssueService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("쿠폰 발급: 실제 발급 시점에 중복 발급")
    void issueCoupon_alreadyIssuedAtIssuance() {
        Member member = createMember(1L);
        Coupon coupon = createCoupon(CouponState.AVAILABLE, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByMember_IdAndCoupon_Id(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> memberCouponIssueService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("쿠폰 발급: 저장 시 유니크 충돌이면 COUPON_ALREADY_EXIST")
    void issueCoupon_duplicateSaveConflict() {
        Member member = createMember(1L);
        Coupon coupon = createCoupon(CouponState.AVAILABLE, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByMember_IdAndCoupon_Id(1L, 1L)).thenReturn(false);
        when(memberCouponRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(MemberCoupon.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> memberCouponIssueService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.COUPON_ALREADY_EXIST);
    }

    @Test
    @DisplayName("한정 쿠폰 발급: 재고 차감 성공 후 저장")
    void issueCoupon_limitedCoupon_decreaseStockSuccess() {
        Member member = createMember(1L);
        Coupon coupon = createLimitedCoupon(1L, CouponState.AVAILABLE, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), 5);
        MemberCoupon saved = MemberCoupon.builder().build();
        ReflectionTestUtils.setField(saved, "id", 200L);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByMember_IdAndCoupon_Id(1L, 1L)).thenReturn(false);
        when(couponRepository.decreaseCouponCount(1L)).thenReturn(1);
        when(memberCouponRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(MemberCoupon.class))).thenReturn(saved);

        Long result = memberCouponIssueService.issueCoupon(1L, 1L);

        assertThat(result).isEqualTo(200L);
        verify(couponRepository).decreaseCouponCount(1L);
        verify(couponIssueOutboxService).enqueueCouponIssued(200L, 1L, 1L);
    }

    @Test
    @DisplayName("한정 쿠폰 발급: 재고 소진이면 COUPON_RUN_OUT")
    void issueCoupon_limitedCoupon_outOfStock() {
        Member member = createMember(1L);
        Coupon coupon = createLimitedCoupon(1L, CouponState.AVAILABLE, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), 1);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByMember_IdAndCoupon_Id(1L, 1L)).thenReturn(false);
        when(couponRepository.decreaseCouponCount(1L)).thenReturn(0);

        assertThatThrownBy(() -> memberCouponIssueService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.COUPON_RUN_OUT);
    }

    @Test
    @DisplayName("쿠폰 발급 여부 확인")
    void isAlreadyIssued() {
        when(memberCouponRepository.existsByMember_IdAndCoupon_Id(1L, 2L)).thenReturn(true);

        boolean result = memberCouponIssueService.isAlreadyIssued(1L, 2L);

        assertThat(result).isTrue();
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

    private Coupon createCoupon(CouponState state, LocalDate validFrom, LocalDate validTo) {
        return Coupon.builder()
                .state(state)
                .validFrom(validFrom)
                .validTo(validTo)
                .build();
    }

    private Coupon createLimitedCoupon(Long id, CouponState state, LocalDate validFrom, LocalDate validTo, int couponCount) {
        return Coupon.builder()
                .id(id)
                .state(state)
                .couponCount(couponCount)
                .validFrom(validFrom)
                .validTo(validTo)
                .build();
    }
}
