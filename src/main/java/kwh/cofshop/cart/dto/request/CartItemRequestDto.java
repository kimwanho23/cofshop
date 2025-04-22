package kwh.cofshop.cart.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemRequestDto {
    @NotNull(message = "아이템 ID는 필수입니다.")
    private Long itemId; // 아이템

    @NotNull(message = "옵션 ID는 필수입니다.")
    private Long OptionId; // 아이템 옵션

    @NotNull(message = "수량을 입력해주세요.")
    private int quantity; // 수량
}
