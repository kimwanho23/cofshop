package kwh.cofshop.coupon.service;

import kwh.cofshop.coupon.dto.request.CreateCouponCommand;
import kwh.cofshop.coupon.repository.CouponRepository;
import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.domain.CouponType;
import kwh.cofshop.coupon.domain.event.CouponCreatedEvent;
import kwh.cofshop.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponStore;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private CouponService couponService;

    @Test
    @DisplayName("쿠폰 ?�성")
    void createCoupon() {
        CreateCouponCommand command = new CreateCouponCommand(
                "?�규 쿠폰",
                null,
                1000,
                null,
                CouponType.FIXED,
                10,
                LocalDate.now(),
                LocalDate.now().plusDays(7)
        );

        Coupon savedCoupon = Coupon.builder()
                .id(1L)
                .couponCount(10)
                .build();

        when(couponStore.save(any(Coupon.class))).thenReturn(savedCoupon);

        Long couponId = couponService.createCoupon(command);

        assertThat(couponId).isEqualTo(1L);
        ArgumentCaptor<CouponCreatedEvent> captor = ArgumentCaptor.forClass(CouponCreatedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().couponId()).isEqualTo(1L);
        assertThat(captor.getValue().couponCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("쿠폰 ?�태 변�? ?�???�음")
    void updateCouponState_notFound() {
        when(couponStore.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.updateCouponState(1L, CouponState.CANCELLED))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode().getCode())
                        .isEqualTo("COUPON-404"));
    }

    @Test
    @DisplayName("쿠폰 ?�태 변�? ?�공")
    void updateCouponState_success() {
        Coupon coupon = Coupon.builder()
                .state(CouponState.AVAILABLE)
                .build();

        when(couponStore.findById(1L)).thenReturn(Optional.of(coupon));

        couponService.updateCouponState(1L, CouponState.EXPIRED);

        assertThat(coupon.getState()).isEqualTo(CouponState.EXPIRED);
    }

    @Test
    @DisplayName("쿠폰 ?�건 조회: ?�???�음")
    void getCouponById_notFound() {
        when(couponStore.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.getCouponById(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("쿠폰 ?�건 조회")
    void getCouponById_success() {
        Coupon coupon = Coupon.builder().build();
        when(couponStore.findById(1L)).thenReturn(Optional.of(coupon));

        Coupon result = couponService.getCouponById(1L);

        assertThat(result).isSameAs(coupon);
    }

    @Test
    @DisplayName("쿠폰 ?�체 조회")
    void getAllCoupons() {
        Coupon coupon = Coupon.builder().build();
        when(couponStore.findAll()).thenReturn(List.of(coupon));

        List<Coupon> results = couponService.getAllCoupons();

        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isSameAs(coupon);
    }

    @Test
    @DisplayName("쿠폰 취소: ?�???�음")
    void cancelCoupon_notFound() {
        when(couponStore.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.cancelCoupon(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("쿠폰 취소: ?�공")
    void cancelCoupon_success() {
        Coupon coupon = Coupon.builder()
                .state(CouponState.AVAILABLE)
                .build();

        when(couponStore.findById(1L)).thenReturn(Optional.of(coupon));

        couponService.cancelCoupon(1L);

        assertThat(coupon.getState()).isEqualTo(CouponState.CANCELLED);
    }

    @Test
    @DisplayName("쿠폰 만료 처리")
    void expireCoupons() {
        when(couponStore.bulkExpireCoupons(any(), any(), any())).thenReturn(3);

        int result = couponService.expireCoupons(LocalDate.now());

        assertThat(result).isEqualTo(3);
    }
}
