package kwh.cofshop.cart.service;

import jakarta.persistence.EntityNotFoundException;
import kwh.cofshop.cart.domain.Cart;
import kwh.cofshop.cart.domain.CartItem;
import kwh.cofshop.cart.dto.response.CartItemResponseDto;
import kwh.cofshop.cart.dto.response.CartResponseDto;
import kwh.cofshop.cart.mapper.CartItemMapper;
import kwh.cofshop.cart.mapper.CartMapper;
import kwh.cofshop.cart.repository.CartItemRepository;
import kwh.cofshop.cart.repository.CartRepository;
import kwh.cofshop.cart.repository.custom.CartRepositoryImpl;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final MemberRepository memberRepository;
    private final CartItemRepository cartItemRepository;

    @Transactional
    public void initializeCartsForExistingMembers() {
        List<Member> membersWithoutCart = memberRepository.findAll().stream()
                .filter(member -> member.getCart() == null)
                .toList();

        for (Member member : membersWithoutCart) {
            Cart newCart = member.createCart();
            cartRepository.save(newCart);
        }
    }

    @Transactional(readOnly = true)
    public CartResponseDto getMemberCartItems(Member member){
        List<CartItemResponseDto> cartItemsByMember = cartRepository.findCartItemsByMember(member);

        CartResponseDto cartResponseDto = new CartResponseDto();
        cartResponseDto.setMemberId(member.getMemberId());
        cartResponseDto.setCartItems(cartItemsByMember);
        return cartResponseDto;
    }

    @Transactional
    public void deleteCartItem(Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(EntityNotFoundException::new);
        cartItemRepository.delete(cartItem);
    }

    @Transactional
    public void deleteCartItemAll(Long cartId) {
        cartRepository.deleteAllByCartId(cartId);
    }

}
