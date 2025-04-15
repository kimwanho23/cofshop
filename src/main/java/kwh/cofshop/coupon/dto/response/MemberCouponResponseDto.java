package kwh.cofshop.coupon.dto.response;

import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.domain.CouponType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class MemberCouponResponseDto {
    private Long memberCouponId;
    private String couponName;
    private CouponType couponType;
    private int discountValue;
    private Integer maxDiscountAmount;
    private int minOrderPrice;
    private CouponState state;
    private LocalDate issuedAt;
    private LocalDate usedAt;
    private LocalDate validFrom;
    private LocalDate validTo;
}
