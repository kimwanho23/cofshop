package kwh.cofshop.cart.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CartRequestDto {

    private Long memberId; // 멤버

    private List<CartItemRequestDto> cartItems; // 장바구니 항목 리스트
}
