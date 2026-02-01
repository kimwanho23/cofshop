package kwh.cofshop.cart.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CartRequestDto {

    @Valid
    @NotEmpty(message = "장바구니 항목은 하나 이상이어야 합니다.")
    private List<CartItemRequestDto> cartItems; // 장바구니 항목 리스트
}
