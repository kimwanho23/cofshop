package kwh.cofshop.coupon.service;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.domain.CouponType;
import kwh.cofshop.coupon.dto.request.CouponRequestDto;
import kwh.cofshop.coupon.dto.response.CouponResponseDto;
import kwh.cofshop.coupon.event.CouponCreatedEvent;
import kwh.cofshop.coupon.mapper.CouponMapper;
import kwh.cofshop.coupon.repository.CouponRepository;
import kwh.cofshop.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

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
    private CouponRepository couponRepository;

    @Mock
    private CouponMapper couponMapper;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private CouponService couponService;

    @Test
    @DisplayName("쿠폰 생성")
    void createCoupon() {
        CouponRequestDto requestDto = new CouponRequestDto();
        requestDto.setName("신규 쿠폰");
        requestDto.setType(CouponType.FIXED);
        requestDto.setDiscountValue(1000);
        requestDto.setValidFrom(LocalDate.now());
        requestDto.setValidTo(LocalDate.now().plusDays(7));
        requestDto.setCouponCount(10);

        Coupon savedCoupon = Coupon.builder()
                .id(1L)
                .couponCount(10)
                .build();

        when(couponRepository.save(any(Coupon.class))).thenReturn(savedCoupon);

        Long couponId = couponService.createCoupon(requestDto);

        assertThat(couponId).isEqualTo(1L);
        ArgumentCaptor<CouponCreatedEvent> captor = ArgumentCaptor.forClass(CouponCreatedEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().couponId()).isEqualTo(1L);
        assertThat(captor.getValue().couponCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("쿠폰 상태 변경: 대상 없음")
    void updateCouponState_notFound() {
        when(couponRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.updateCouponState(1L, CouponState.CANCELLED))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("쿠폰 상태 변경: 성공")
    void updateCouponState_success() {
        Coupon coupon = Coupon.builder()
                .state(CouponState.AVAILABLE)
                .build();

        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        couponService.updateCouponState(1L, CouponState.EXPIRED);

        assertThat(coupon.getState()).isEqualTo(CouponState.EXPIRED);
    }

    @Test
    @DisplayName("쿠폰 단건 조회: 대상 없음")
    void getCouponById_notFound() {
        when(couponRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.getCouponById(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("쿠폰 단건 조회")
    void getCouponById_success() {
        Coupon coupon = Coupon.builder().build();
        CouponResponseDto responseDto = new CouponResponseDto();

        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(couponMapper.toResponseDto(coupon)).thenReturn(responseDto);

        CouponResponseDto result = couponService.getCouponById(1L);

        assertThat(result).isSameAs(responseDto);
    }

    @Test
    @DisplayName("쿠폰 전체 조회")
    void getAllCoupons() {
        Coupon coupon = Coupon.builder().build();
        when(couponRepository.findAll()).thenReturn(List.of(coupon));
        when(couponMapper.toResponseDto(coupon)).thenReturn(new CouponResponseDto());

        List<CouponResponseDto> results = couponService.getAllCoupons();

        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("쿠폰 취소: 대상 없음")
    void cancelCoupon_notFound() {
        when(couponRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.cancelCoupon(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("쿠폰 취소: 성공")
    void cancelCoupon_success() {
        Coupon coupon = Coupon.builder()
                .state(CouponState.AVAILABLE)
                .build();

        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));

        couponService.cancelCoupon(1L);

        assertThat(coupon.getState()).isEqualTo(CouponState.CANCELLED);
    }

    @Test
    @DisplayName("쿠폰 만료 처리")
    void expireCoupons() {
        when(couponRepository.bulkExpireCoupons(any(), any())).thenReturn(3);

        int result = couponService.expireCoupons(LocalDate.now());

        assertThat(result).isEqualTo(3);
    }
}