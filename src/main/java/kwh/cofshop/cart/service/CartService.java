package kwh.cofshop.cart.service;

import jakarta.persistence.EntityNotFoundException;
import kwh.cofshop.cart.domain.Cart;
import kwh.cofshop.cart.domain.CartItem;
import kwh.cofshop.cart.dto.request.CartItemRequestDto;
import kwh.cofshop.cart.dto.request.CartRequestDto;
import kwh.cofshop.cart.dto.response.CartItemResponseDto;
import kwh.cofshop.cart.dto.response.CartResponseDto;
import kwh.cofshop.cart.mapper.CartItemMapper;
import kwh.cofshop.cart.mapper.CartMapper;
import kwh.cofshop.cart.repository.CartItemRepository;
import kwh.cofshop.cart.repository.CartRepository;
import kwh.cofshop.cart.repository.custom.CartRepositoryImpl;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
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

    private final CartItemMapper cartItemMapper;

    // 장바구니 시스템이 없는 유저에게 장바구니를 추가했다.
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

    // 장바구니에 아이템 등록
    @Transactional
    public CartItemResponseDto addCartItem(CartItemRequestDto cartItemRequestDto){
        Optional<CartItem> existingCartItem = cartItemRepository.findByItemOptionIdAndItemId(
                cartItemRequestDto.getOptionId(), cartItemRequestDto.getItemId());
        if (existingCartItem.isPresent()) {
            // 이미 존재하는 경우 수량 증가
            CartItem cartItem = existingCartItem.get();
            cartItem.addQuantity(cartItemRequestDto.getQuantity());
            return cartItemMapper.toResponseDto(cartItem);
        }
        CartItem save = cartItemRepository.save(cartItemMapper.toEntity(cartItemRequestDto));
        return cartItemMapper.toResponseDto(save);
    }


    // 회원 장바구니 조회 ( 장바구니에 있는 물건 )
    @Transactional(readOnly = true)
    public CartResponseDto getMemberCartItems(Member member){
        List<CartItemResponseDto> cartItemsByMember = cartRepository.findCartItemsByMember(member);

        CartResponseDto cartResponseDto = new CartResponseDto();
        cartResponseDto.setMemberId(member.getMemberId());
        cartResponseDto.setCartItems(cartItemsByMember);
        return cartResponseDto;
    }

    // 장바구니 아이템 삭제
    @Transactional
    public void deleteCartItem(Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ITEM_NOT_FOUND));
        cartItemRepository.delete(cartItem);
    }

    // 장바구니 전체 삭제
    @Transactional
    public void deleteCartItemAll(Long cartId) {
        cartRepository.deleteAllByCartId(cartId);
    }

}
