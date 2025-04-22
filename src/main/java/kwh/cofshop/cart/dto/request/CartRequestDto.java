package kwh.cofshop.cart.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CartRequestDto {

    @NotNull(message = "회원 ID는 필수입니다.")
    private Long memberId; // 멤버

    private List<CartItemRequestDto> cartItems; // 장바구니 항목 리스트
}
