package kwh.cofshop.coupon.service;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.dto.request.CouponRequestDto;
import kwh.cofshop.coupon.dto.response.CouponResponseDto;
import kwh.cofshop.coupon.event.CouponCreatedEvent;
import kwh.cofshop.coupon.mapper.CouponMapper;
import kwh.cofshop.coupon.repository.CouponRepository;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponMapper couponMapper;

    private final ApplicationEventPublisher applicationEventPublisher;

    // 쿠폰 생성
    @Transactional
    public Long createCoupon(CouponRequestDto couponRequestDto){
        Coupon savedCoupon = couponRepository.save(Coupon.createCoupon(couponRequestDto));

        applicationEventPublisher.publishEvent(new CouponCreatedEvent(savedCoupon.getId(), savedCoupon.getCouponCount()));

        return savedCoupon.getId();
    }

    // 쿠폰 상태 변경
    @Transactional
    public void updateCouponState(Long couponId, CouponState newState) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.COUPON_NOT_AVAILABLE));

        coupon.updateCouponState(newState);
    }

    // 쿠폰 단건 조회
    @Transactional(readOnly = true)
    public CouponResponseDto getCouponById(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.COUPON_NOT_FOUND));
        return couponMapper.toResponseDto(coupon);
    }

    // 쿠폰 전체 조회
    @Transactional(readOnly = true)
    public List<CouponResponseDto> getAllCoupons() {
        return couponRepository.findAll().stream()
                .map(couponMapper::toResponseDto)
                .toList();
    }

    // 쿠폰 발급 취소
    @Transactional
    public void cancelCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.COUPON_NOT_FOUND));
        coupon.updateCouponState(CouponState.CANCELLED);
    }

    // 쿠폰 만료
    @Transactional
    public int expireCoupons(LocalDate now) {
        return couponRepository.bulkExpireCoupons(now, CouponState.EXPIRED.name()); // 만료 처리
    }
}
