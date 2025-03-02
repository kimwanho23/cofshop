package kwh.cofshop.cart.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemRequestDto {
    private Long itemId; // 아이템
    private Long OptionId; // 아이템 옵션
    private int quantity; // 수량
}
