package kwh.cofshop.cart.repository.custom;


import kwh.cofshop.cart.dto.response.CartItemResponseDto;
import kwh.cofshop.member.domain.Member;

import java.util.List;

public interface CartRepositoryCustom {

    List<CartItemResponseDto> findCartItemsByMember(String email);

    void deleteAllByCartId(Long cartId);
}
