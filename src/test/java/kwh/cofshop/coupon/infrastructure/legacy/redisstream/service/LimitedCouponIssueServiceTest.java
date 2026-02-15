package kwh.cofshop.coupon.infrastructure.legacy.redisstream.service;

import kwh.cofshop.coupon.application.service.MemberCouponIssueService;
import kwh.cofshop.coupon.domain.CouponIssueState;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LimitedCouponIssueServiceTest {

    @Mock
    private CouponRedisService couponRedisService;

    @Mock
    private MemberCouponIssueService memberCouponIssueService;

    @InjectMocks
    private LimitedCouponIssueService limitedCouponIssueService;

    @Test
    @DisplayName("DB에 이미 발급된 경우 Redis 표식만 보정하고 ALREADY_ISSUED 반환")
    void issueCoupon_alreadyIssuedInDb() {
        when(memberCouponIssueService.isAlreadyIssued(1L, 10L)).thenReturn(true);

        CouponIssueState result = limitedCouponIssueService.issueCoupon(10L, 1L);

        assertThat(result).isEqualTo(CouponIssueState.ALREADY_ISSUED);
        verify(couponRedisService).saveIssued(10L, 1L);
        verify(couponRedisService, never()).issueCoupon(10L, 1L);
    }

    @Test
    @DisplayName("Redis 선점 결과가 SUCCESS가 아니면 그대로 반환")
    void issueCoupon_notSuccessFromRedis() {
        when(memberCouponIssueService.isAlreadyIssued(1L, 10L)).thenReturn(false);
        when(couponRedisService.issueCoupon(10L, 1L)).thenReturn(CouponIssueState.OUT_OF_STOCK);

        CouponIssueState result = limitedCouponIssueService.issueCoupon(10L, 1L);

        assertThat(result).isEqualTo(CouponIssueState.OUT_OF_STOCK);
    }

    @Test
    @DisplayName("Redis 선점 성공 후 DB 저장 성공 시 SUCCESS 반환")
    void issueCoupon_success() {
        when(memberCouponIssueService.isAlreadyIssued(1L, 10L)).thenReturn(false);
        when(couponRedisService.issueCoupon(10L, 1L)).thenReturn(CouponIssueState.SUCCESS);
        when(memberCouponIssueService.issueCoupon(1L, 10L)).thenReturn(100L);

        CouponIssueState result = limitedCouponIssueService.issueCoupon(10L, 1L);

        assertThat(result).isEqualTo(CouponIssueState.SUCCESS);
        verify(memberCouponIssueService).issueCoupon(1L, 10L);
    }

    @Test
    @DisplayName("Redis 선점 성공 후 DB 저장 실패 시 Redis 롤백")
    void issueCoupon_dbSaveFailRollback() {
        when(memberCouponIssueService.isAlreadyIssued(1L, 10L)).thenReturn(false);
        when(couponRedisService.issueCoupon(10L, 1L)).thenReturn(CouponIssueState.SUCCESS);
        when(memberCouponIssueService.issueCoupon(1L, 10L)).thenThrow(new RuntimeException("db fail"));

        assertThatThrownBy(() -> limitedCouponIssueService.issueCoupon(10L, 1L))
                .isInstanceOf(RuntimeException.class);

        verify(couponRedisService).rollbackIssuedCoupon(10L, 1L);
    }

    @Test
    @DisplayName("Redis 선점 성공 후 DB 유니크 충돌이면 ALREADY_ISSUED로 종료")
    void issueCoupon_duplicateConflictReturnsAlreadyIssued() {
        when(memberCouponIssueService.isAlreadyIssued(1L, 10L)).thenReturn(false);
        when(couponRedisService.issueCoupon(10L, 1L)).thenReturn(CouponIssueState.SUCCESS);
        when(memberCouponIssueService.issueCoupon(1L, 10L))
                .thenThrow(new BusinessException(BusinessErrorCode.COUPON_ALREADY_EXIST));

        CouponIssueState result = limitedCouponIssueService.issueCoupon(10L, 1L);

        assertThat(result).isEqualTo(CouponIssueState.ALREADY_ISSUED);
        verify(couponRedisService).rollbackIssuedCoupon(10L, 1L);
    }
}
