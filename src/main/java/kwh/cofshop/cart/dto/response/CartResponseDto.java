package kwh.cofshop.cart.dto.response;


import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CartResponseDto {

    private String email; // 멤버 정보

    private List<CartItemResponseDto> cartItems; // 장바구니 항목 리스트
}
