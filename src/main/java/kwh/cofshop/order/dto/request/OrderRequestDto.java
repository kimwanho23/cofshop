package kwh.cofshop.order.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderRequestDto {

    @Valid
    @NotNull(message = "배송지는 필수입니다.")
    private AddressRequestDto address;

    @Size(max = 500, message = "배송 요청사항은 500자 이하여야 합니다.")
    private String deliveryRequest;

    @Valid
    @NotEmpty(message = "주문 항목은 하나 이상이어야 합니다.")
    private List<OrderItemRequestDto> orderItems;

    @Positive(message = "회원 쿠폰 ID는 1 이상이어야 합니다.")
    private Long memberCouponId;

    @PositiveOrZero(message = "사용 포인트는 0 이상이어야 합니다.")
    private Integer usePoint; // 사용 포인트
}
