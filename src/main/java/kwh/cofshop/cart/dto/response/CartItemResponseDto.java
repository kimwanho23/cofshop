package kwh.cofshop.cart.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemResponseDto {

    private Long cartId;
    private int quantity;
    private Long itemId;
    private Long optionId;
}
