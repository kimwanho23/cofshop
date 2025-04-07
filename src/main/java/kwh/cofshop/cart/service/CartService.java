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

    private final CartMapper cartMapper;

    // 장바구니가 없다면 생성을 해준다.
    @Transactional
    public CartResponseDto createCart(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.MEMBER_NOT_FOUND));

        Cart cart = member.getCart();

        if (cart == null) {
            cart = cartRepository.save(member.createCart());
        }
        return cartMapper.toResponseDto(cart);
    }

    // 장바구니의 존재 여부 확인
    @Transactional(readOnly = true)
    public boolean checkCartExistByMemberId(Long memberId) {
        return cartRepository.existsByMemberId(memberId);
    }

    @Transactional
    public void deleteByMemberId(Long memberId) {
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.CART_NOT_FOUND));
        cartRepository.delete(cart);
    }
}
