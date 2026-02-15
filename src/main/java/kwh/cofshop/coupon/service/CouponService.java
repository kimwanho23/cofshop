package kwh.cofshop.coupon.service;

import kwh.cofshop.coupon.domain.event.CouponCreatedEvent;
import kwh.cofshop.coupon.dto.request.CreateCouponCommand;
import kwh.cofshop.coupon.repository.CouponRepository;
import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    private final ApplicationEventPublisher applicationEventPublisher;

    // 쿠폰 ?�성
    @Transactional
    public Long createCoupon(CreateCouponCommand command) {
        Coupon savedCoupon = couponRepository.save(
                Coupon.createCoupon(
                        command.name(),
                        command.minOrderPrice(),
                        command.discountValue(),
                        command.maxDiscountAmount(),
                        command.type(),
                        command.couponCount(),
                        command.validFrom(),
                        command.validTo()
                )
        );

        applicationEventPublisher.publishEvent(new CouponCreatedEvent(savedCoupon.getId(), savedCoupon.getCouponCount()));

        return savedCoupon.getId();
    }

    // 쿠폰 ?�태 변�?
    @Transactional
    public void updateCouponState(Long couponId, CouponState newState) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.COUPON_NOT_FOUND));

        coupon.updateCouponState(newState);
    }

    // 쿠폰 ?�건 조회
    @Transactional(readOnly = true)
    public Coupon getCouponById(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.COUPON_NOT_FOUND));
    }

    // 쿠폰 ?�체 조회
    @Transactional(readOnly = true)
    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
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
        return couponRepository.bulkExpireCoupons(now, CouponState.EXPIRED, CouponState.AVAILABLE); // 만료 처리
    }
}
