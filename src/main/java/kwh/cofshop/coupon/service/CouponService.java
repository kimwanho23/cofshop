package kwh.cofshop.coupon.service;

import kwh.cofshop.coupon.application.command.CreateCouponCommand;
import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.domain.CouponType;
import kwh.cofshop.coupon.dto.response.CouponResponseDto;
import kwh.cofshop.coupon.repository.CouponRepository;
import kwh.cofshop.coupon.repository.projection.CouponReadProjection;
import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional
    public Long createCoupon(CreateCouponCommand command) {
        validateCouponCreateCommand(command);

        Coupon savedCoupon = couponRepository.save(
                Coupon.createCoupon(
                        command.getName(),
                        command.getMinOrderPrice(),
                        command.getDiscountValue(),
                        command.getMaxDiscountAmount(),
                        command.getType(),
                        command.getCouponCount(),
                        command.getValidFrom(),
                        command.getValidTo()
                )
        );

        return savedCoupon.getId();
    }

    private void validateCouponCreateCommand(CreateCouponCommand command) {
        if (command.getValidFrom().isAfter(command.getValidTo())) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }

        Integer couponCount = command.getCouponCount();
        if (couponCount != null && couponCount <= 0) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }

        Integer minOrderPrice = command.getMinOrderPrice();
        if (minOrderPrice != null && minOrderPrice < 0) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }

        Integer discountValue = command.getDiscountValue();
        if (discountValue == null || discountValue <= 0) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }

        Integer maxDiscountAmount = command.getMaxDiscountAmount();
        if (maxDiscountAmount != null && maxDiscountAmount < 0) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }

        if (command.getType() == CouponType.RATE && discountValue > 100) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
    }

    @Transactional
    public void updateCouponState(Long couponId, CouponState newState) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.COUPON_NOT_FOUND));
        coupon.updateCouponState(newState);
    }

    @Transactional(readOnly = true)
    public CouponResponseDto getCouponById(Long couponId) {
        CouponReadProjection projection = couponRepository.findCouponReadProjectionById(couponId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.COUPON_NOT_FOUND));
        return toResponseDto(projection);
    }

    @Transactional(readOnly = true)
    public List<CouponResponseDto> getAllCoupons() {
        return couponRepository.findAllProjectedBy().stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    public void cancelCoupon(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.COUPON_NOT_FOUND));
        coupon.updateCouponState(CouponState.CANCELLED);
    }

    @Transactional
    public int expireCoupons(LocalDate now) {
        return couponRepository.bulkExpireCoupons(now, CouponState.EXPIRED, CouponState.AVAILABLE);
    }

    private CouponResponseDto toResponseDto(CouponReadProjection projection) {
        CouponResponseDto responseDto = new CouponResponseDto();
        responseDto.setId(projection.getId());
        responseDto.setName(projection.getName());
        responseDto.setType(projection.getType());
        responseDto.setDiscountValue(projection.getDiscountValue());
        responseDto.setMaxDiscountAmount(projection.getMaxDiscount());
        responseDto.setMinOrderPrice(projection.getMinOrderPrice());
        responseDto.setValidFrom(projection.getValidFrom());
        responseDto.setValidTo(projection.getValidTo());
        responseDto.setCreatedAt(
                projection.getCouponCreatedAt() == null ? null : projection.getCouponCreatedAt().atStartOfDay()
        );
        return responseDto;
    }
}
