package kwh.cofshop.cart.repository.custom;

import kwh.cofshop.cart.domain.CartItem;
import kwh.cofshop.cart.dto.response.CartItemResponseDto;

import java.util.List;
import java.util.Optional;

public interface CartItemRepositoryCustom {

    List<CartItemResponseDto> findCartItemsByMemberId(Long id);

    void deleteAllByCartId(Long cartId);

    Optional<CartItem> findByItemAndOptionAndCart(Long itemId, Long optionId, Long cartId);

}
