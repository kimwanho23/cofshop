package kwh.cofshop.coupon.domain;

import jakarta.persistence.*;
import kwh.cofshop.coupon.policy.discount.CouponDiscountPolicy;
import kwh.cofshop.coupon.policy.discount.CouponFixedDiscountPolicy;
import kwh.cofshop.coupon.policy.discount.CouponRateDiscountPolicy;
import kwh.cofshop.coupon.dto.request.CouponRequestDto;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long id;

    @Column(name = "coupon_name", nullable = false)
    private String name; // 쿠폰명

    private Integer minOrderPrice; // 해당 금액 이상만 사용 가능

    private int discountValue; //할인율 / 할인 금액

    private Integer maxDiscount; // 최대 할인 금액 제한

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CouponState state;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CouponType type; // FIXED, PERCENTAGE

    @Column(nullable = false)
    private LocalDate couponCreatedAt; // 쿠폰 생성일

    private Integer couponCount; // 남은 수량 (null이면 무제한)

    @Column(nullable = false)
    private LocalDate validFrom; // 쿠폰 유효 시작일

    @Column(nullable = false)
    private LocalDate validTo; // 쿠폰 유효 종료일 (null 가능)

    @Builder
    public Coupon(Long id, String name, Integer minOrderPrice, int discountValue, Integer maxDiscount,
                  CouponState state, CouponType type, LocalDate couponCreatedAt, Integer couponCount, LocalDate validFrom, LocalDate validTo) {
        this.id = id;
        this.name = name;
        this.minOrderPrice = minOrderPrice;
        this.discountValue = discountValue;
        this.maxDiscount = maxDiscount;
        this.state = state;
        this.type = type;
        this.couponCreatedAt = couponCreatedAt;
        this.couponCount = couponCount;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public static Coupon createCoupon(CouponRequestDto dto) {
        return Coupon.builder()
                .name(dto.getName())
                .minOrderPrice(dto.getMinOrderPrice())
                .discountValue(dto.getDiscountValue())
                .maxDiscount(dto.getMaxDiscountAmount())
                .type(dto.getType())
                .state(CouponState.AVAILABLE)
                .couponCreatedAt(LocalDate.now())
                .couponCount(dto.getCouponCount())
                .validFrom(dto.getValidFrom())
                .validTo(dto.getValidTo())
                .build();
    }

    public void updateCouponState(CouponState newState) {
        this.state = newState;
    }

    // 수량 제한 여부
    public boolean isLimitedQuantity() {
        return this.couponCount != null;
    }

/*    public void decreaseCouponCount() {
        if (this.couponCount == null)
            return; // 무한 수량일 시 아무것도 하지 않음.
        if (this.couponCount <= 0) {
            throw new BusinessException(BusinessErrorCode.COUPON_RUN_OUT); // 쿠폰 소진
        }
        this.couponCount -= 1;
    }*/

}
