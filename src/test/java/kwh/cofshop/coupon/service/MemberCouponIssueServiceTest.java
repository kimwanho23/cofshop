package kwh.cofshop.coupon.service;

import kwh.cofshop.coupon.service.outbox.CouponIssueOutboxService;
import kwh.cofshop.coupon.repository.CouponRepository;
import kwh.cofshop.coupon.repository.MemberCouponRepository;
import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.member.api.MemberReadPort;
import kwh.cofshop.member.domain.Member;
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
    private CouponRepository couponStore;

    @Mock
    private MemberReadPort memberReadPort;

    @Mock
    private CouponIssueOutboxService couponIssueOutbox;

    @InjectMocks
    private MemberCouponIssueService memberCouponIssueService;

    @Test
    @DisplayName("쿠폰 발급: ?�원 ?�음")
    void issueCoupon_memberNotFound() {
        when(memberReadPort.getById(anyLong())).thenThrow(new BusinessException(BusinessErrorCode.MEMBER_NOT_FOUND));

        assertThatThrownBy(() -> memberCouponIssueService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("쿠폰 발급: 쿠폰 ?�음")
    void issueCoupon_couponNotFound() {
        Member member = createMember(1L);
        when(memberReadPort.getById(1L)).thenReturn(member);
        when(couponStore.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberCouponIssueService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("쿠폰 발급: ?�공")
    void issueCoupon_success() {
        Member member = createMember(1L);
        Coupon coupon = createCoupon(CouponState.AVAILABLE, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));
        MemberCoupon saved = MemberCoupon.builder().build();
        ReflectionTestUtils.setField(saved, "id", 100L);

        when(memberReadPort.getById(1L)).thenReturn(member);
        when(couponStore.findById(1L)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByMember_IdAndCoupon_Id(1L, 1L)).thenReturn(false);
        when(memberCouponRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(MemberCoupon.class))).thenReturn(saved);

        Long result = memberCouponIssueService.issueCoupon(1L, 1L);

        assertThat(result).isEqualTo(100L);
        verify(couponIssueOutbox).enqueueCouponIssued(100L, 1L, 1L);
    }

    @Test
    @DisplayName("쿠폰 발급: ?�제 발급 ?�점??쿠폰 ?�태가 ?�용 불�?")
    void issueCoupon_couponNotAvailableState() {
        Member member = createMember(1L);
        Coupon coupon = createCoupon(CouponState.CANCELLED, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));

        when(memberReadPort.getById(1L)).thenReturn(member);
        when(couponStore.findById(1L)).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> memberCouponIssueService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("쿠폰 발급: ?�제 발급 ?�점??쿠폰 만료")
    void issueCoupon_couponExpiredAtIssuance() {
        Member member = createMember(1L);
        Coupon coupon = createCoupon(CouponState.AVAILABLE, LocalDate.now().minusDays(10), LocalDate.now().minusDays(1));

        when(memberReadPort.getById(1L)).thenReturn(member);
        when(couponStore.findById(1L)).thenReturn(Optional.of(coupon));

        assertThatThrownBy(() -> memberCouponIssueService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("쿠폰 발급: ?�제 발급 ?�점??중복 발급")
    void issueCoupon_alreadyIssuedAtIssuance() {
        Member member = createMember(1L);
        Coupon coupon = createCoupon(CouponState.AVAILABLE, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));

        when(memberReadPort.getById(1L)).thenReturn(member);
        when(couponStore.findById(1L)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByMember_IdAndCoupon_Id(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> memberCouponIssueService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("쿠폰 발급: ?�?????�니??충돌?�면 COUPON_ALREADY_EXIST")
    void issueCoupon_duplicateSaveConflict() {
        Member member = createMember(1L);
        Coupon coupon = createCoupon(CouponState.AVAILABLE, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));

        when(memberReadPort.getById(1L)).thenReturn(member);
        when(couponStore.findById(1L)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByMember_IdAndCoupon_Id(1L, 1L)).thenReturn(false);
        when(memberCouponRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(MemberCoupon.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> memberCouponIssueService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.COUPON_ALREADY_EXIST);
    }

    @Test
    @DisplayName("Limited coupon decrements stock and issues")
    void issueCoupon_limitedCoupon_decreaseStockSuccess() {
        Member member = createMember(1L);
        Coupon coupon = createLimitedCoupon(1L, CouponState.AVAILABLE, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), 5);
        MemberCoupon saved = MemberCoupon.builder().build();
        ReflectionTestUtils.setField(saved, "id", 200L);

        when(memberReadPort.getById(1L)).thenReturn(member);
        when(couponStore.findById(1L)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByMember_IdAndCoupon_Id(1L, 1L)).thenReturn(false);
        when(couponStore.decreaseCouponCount(1L)).thenReturn(1);
        when(memberCouponRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(MemberCoupon.class))).thenReturn(saved);

        Long result = memberCouponIssueService.issueCoupon(1L, 1L);

        assertThat(result).isEqualTo(200L);
        verify(couponStore).decreaseCouponCount(1L);
        verify(couponIssueOutbox).enqueueCouponIssued(200L, 1L, 1L);
    }

    @Test
    @DisplayName("?�정 쿠폰 발급: ?�고 ?�진?�면 COUPON_RUN_OUT")
    void issueCoupon_limitedCoupon_outOfStock() {
        Member member = createMember(1L);
        Coupon coupon = createLimitedCoupon(1L, CouponState.AVAILABLE, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), 1);

        when(memberReadPort.getById(1L)).thenReturn(member);
        when(couponStore.findById(1L)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByMember_IdAndCoupon_Id(1L, 1L)).thenReturn(false);
        when(couponStore.decreaseCouponCount(1L)).thenReturn(0);

        assertThatThrownBy(() -> memberCouponIssueService.issueCoupon(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.COUPON_RUN_OUT);
    }

    @Test
    @DisplayName("쿠폰 발급 ?��? ?�인")
    void isAlreadyIssued() {
        when(memberCouponRepository.existsByMember_IdAndCoupon_Id(1L, 2L)).thenReturn(true);

        boolean result = memberCouponIssueService.isAlreadyIssued(1L, 2L);

        assertThat(result).isTrue();
    }

    private Member createMember(Long id) {
        return Member.builder()
                .id(id)
                .email("user" + id + "@example.com")
                .memberName("user")
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
