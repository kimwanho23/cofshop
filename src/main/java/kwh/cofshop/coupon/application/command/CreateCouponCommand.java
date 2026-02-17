package kwh.cofshop.coupon.application.command;

import kwh.cofshop.coupon.domain.CouponType;

import java.time.LocalDate;
import java.util.Objects;

public final class CreateCouponCommand {

    private final String name;
    private final Integer minOrderPrice;
    private final Integer discountValue;
    private final Integer maxDiscountAmount;
    private final CouponType type;
    private final Integer couponCount;
    private final LocalDate validFrom;
    private final LocalDate validTo;

    private CreateCouponCommand(
            String name,
            Integer minOrderPrice,
            Integer discountValue,
            Integer maxDiscountAmount,
            CouponType type,
            Integer couponCount,
            LocalDate validFrom,
            LocalDate validTo
    ) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.minOrderPrice = minOrderPrice;
        this.discountValue = Objects.requireNonNull(discountValue, "discountValue must not be null");
        this.maxDiscountAmount = maxDiscountAmount;
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.couponCount = couponCount;
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom must not be null");
        this.validTo = Objects.requireNonNull(validTo, "validTo must not be null");
    }

    public static CreateCouponCommand of(
            String name,
            Integer minOrderPrice,
            Integer discountValue,
            Integer maxDiscountAmount,
            CouponType type,
            Integer couponCount,
            LocalDate validFrom,
            LocalDate validTo
    ) {
        return new CreateCouponCommand(
                name,
                minOrderPrice,
                discountValue,
                maxDiscountAmount,
                type,
                couponCount,
                validFrom,
                validTo
        );
    }

    public String getName() {
        return name;
    }

    public Integer getMinOrderPrice() {
        return minOrderPrice;
    }

    public Integer getDiscountValue() {
        return discountValue;
    }

    public Integer getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public CouponType getType() {
        return type;
    }

    public Integer getCouponCount() {
        return couponCount;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }
}
