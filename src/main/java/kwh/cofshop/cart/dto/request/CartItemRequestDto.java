package kwh.cofshop.cart.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemRequestDto {
    @NotNull(message = "아이템 ID는 필수입니다.")
    private Long itemId; // 아이템

    @NotNull(message = "옵션 ID는 필수입니다.")
    private Long optionId; // 아이템 옵션

    @Positive(message = "수량은 1 이상이어야 합니다.")
    private int quantity; // 수량
}
