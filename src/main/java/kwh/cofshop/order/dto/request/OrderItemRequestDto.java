package kwh.cofshop.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemRequestDto { // 주문 상품

    @NotNull(message = "상품 ID는 필수입니다.")
    private Long itemId; // 상품 ID

    @NotNull(message = "옵션 ID는 필수입니다.")
    private Long optionId; // 옵션 ID

    @Positive(message = "수량은 1 이상이어야 합니다.")
    private int quantity;  // 수량
}
