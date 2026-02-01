package kwh.cofshop.coupon.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class MemberCouponRequestDto {

    @NotNull(message = "쿠폰 ID는 필수입니다.")
    private Long couponId;
}
