package kwh.cofshop.coupon.dto.response;

import kwh.cofshop.coupon.domain.CouponState;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class MemberCouponResponseDto {
    private Long memberCouponId;
    private Long memberId;
    private Long couponId;
    private CouponState state;
    private LocalDate issuedAt;
    private LocalDate usedAt;
    private LocalDate expiredAt;

    public MemberCouponResponseDto(Long memberCouponId, Long memberId, Long couponId, CouponState state,
                                   LocalDate issuedAt, LocalDate usedAt, LocalDate expiredAt) {
        this.memberCouponId = memberCouponId;
        this.memberId = memberId;
        this.couponId = couponId;
        this.state = state;
        this.issuedAt = issuedAt;
        this.usedAt = usedAt;
        this.expiredAt = expiredAt;
    }
}
