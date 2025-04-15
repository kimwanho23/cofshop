package kwh.cofshop.coupon.domain;

import jakarta.persistence.*;
import kwh.cofshop.coupon.policy.CouponDiscountPolicy;
import kwh.cofshop.coupon.policy.CouponFixedDiscountPolicy;
import kwh.cofshop.coupon.policy.CouponRateDiscountPolicy;
import kwh.cofshop.coupon.dto.request.CouponRequestDto;
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

    @Enumerated(EnumType.STRING)
    private CouponState state;

    @Enumerated(EnumType.STRING)
    private CouponType type; // FIXED, PERCENTAGE

    @Column(nullable = false)
    private LocalDate couponCreatedAt; // 쿠폰 생성일

    @Column(nullable = false)
    private LocalDate validFrom; // 쿠폰 유효 시작일

    @Column(nullable = false)
    private LocalDate validTo; // 쿠폰 유효 종료일

    @Builder
    public Coupon(Long id, String name, int discountValue, Integer maxDiscount, CouponState state,
                  CouponType type, LocalDate couponCreatedAt, LocalDate validFrom, LocalDate validTo) {
        this.id = id;
        this.name = name;
        this.discountValue = discountValue;
        this.maxDiscount = maxDiscount;
        this.state = state;
        this.type = type;
        this.couponCreatedAt = couponCreatedAt;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public static Coupon createCoupon(CouponRequestDto dto) {
        return Coupon.builder()
                .name(dto.getName())
                .discountValue(dto.getDiscountValue())
                .maxDiscount(dto.getMaxDiscountAmount())
                .type(dto.getType())
                .state(CouponState.AVAILABLE)
                .couponCreatedAt(LocalDate.now())
                .validFrom(dto.getValidFrom())
                .validTo(dto.getValidTo())
                .build();
    }

    public void updateCouponState(CouponState newState) {
        this.state = newState;
    }


    public CouponDiscountPolicy getPolicy() {
        return switch (this.type) {
            case FIXED      -> new CouponFixedDiscountPolicy(this.discountValue);
            case RATE -> new CouponRateDiscountPolicy(this.discountValue, this.maxDiscount);
        };
    }

    public int calculateDiscount(int targetPrice) {
        return getPolicy().calculateDiscount(targetPrice);
    }

}
