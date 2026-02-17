package kwh.cofshop.coupon.repository.projection;

import kwh.cofshop.coupon.domain.CouponType;

import java.time.LocalDate;

public interface CouponReadProjection {

    Long getId();

    String getName();

    CouponType getType();

    int getDiscountValue();

    Integer getMaxDiscount();

    Integer getMinOrderPrice();

    LocalDate getValidFrom();

    LocalDate getValidTo();

    LocalDate getCouponCreatedAt();
}
