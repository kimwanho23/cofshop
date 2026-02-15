package kwh.cofshop.coupon.dto.response;

import kwh.cofshop.coupon.domain.CouponState;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class MemberCouponResponseDto {
    private Long memberCouponId;
    private Long memberId;
    private Long couponId;
    private CouponState state;
    private LocalDate issuedAt; // 발급??
    private LocalDate usedAt; // ?�용??
    private LocalDate expiredAt; // 만료??
}
