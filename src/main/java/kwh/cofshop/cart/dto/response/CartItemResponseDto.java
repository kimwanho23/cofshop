package kwh.cofshop.cart.dto.response;

import kwh.cofshop.item.dto.response.ItemOptionResponseDto;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemResponseDto {

    private Long cartItemId;
    private int quantity;
    private ItemResponseDto item;
    private ItemOptionResponseDto itemOption;
}
