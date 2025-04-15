package kwh.cofshop.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import kwh.cofshop.order.domain.Address;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderRequestDto {

    @NotNull(message = "배송지는 필수입니다.")
    private Address address;

    private String deliveryRequest;

    @NotEmpty(message = "주문 항목은 하나 이상이어야 합니다.")
    private List<OrderItemRequestDto> orderItemRequestDtoList;

    private Long memberCouponId;

    private int discountFromCoupon; // 쿠폰 할인 금액

    private Integer usePoint; // 사용 포인트
}
