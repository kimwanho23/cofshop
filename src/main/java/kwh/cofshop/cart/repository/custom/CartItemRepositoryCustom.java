package kwh.cofshop.cart.repository.custom;

import kwh.cofshop.cart.dto.response.CartItemResponseDto;

import java.util.List;

public interface CartItemRepositoryCustom {

    List<CartItemResponseDto> findCartItemsByMember(Long id);

    void deleteAllByCartId(Long cartId);
}
