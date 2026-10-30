package kwh.cofshop.coupon.domain;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberCouponTest {

    @Test
    @DisplayName("쿠폰 사용: AVAILABLE 상태에서 USED로 전이")
    void useCoupon_success() {
        MemberCoupon memberCoupon = MemberCoupon.builder()
                .state(CouponState.AVAILABLE)
                .build();

        memberCoupon.useCoupon();

        assertThat(memberCoupon.getState()).isEqualTo(CouponState.USED);
        assertThat(memberCoupon.getUsedAt()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("쿠폰 사용: AVAILABLE 상태가 아니면 실패")
    void useCoupon_invalidState() {
        MemberCoupon memberCoupon = MemberCoupon.builder()
                .state(CouponState.USED)
                .usedAt(LocalDate.now())
                .build();

        assertThatThrownBy(memberCoupon::useCoupon)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.COUPON_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("쿠폰 복구: USED -> AVAILABLE 전이 시 usedAt 초기화")
    void restoreCouponStatus_clearUsedAt() {
        MemberCoupon memberCoupon = MemberCoupon.builder()
                .state(CouponState.USED)
                .usedAt(LocalDate.now())
                .build();

        memberCoupon.restoreCouponStatus();

        assertThat(memberCoupon.getState()).isEqualTo(CouponState.AVAILABLE);
        assertThat(memberCoupon.getUsedAt()).isNull();
    }
}
