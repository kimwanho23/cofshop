package kwh.cofshop.cart.service;

import kwh.cofshop.cart.domain.Cart;
import kwh.cofshop.cart.dto.response.CartResponseDto;
import kwh.cofshop.cart.mapper.CartMapper;
import kwh.cofshop.cart.repository.CartItemRepository;
import kwh.cofshop.cart.repository.CartRepository;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.member.api.MemberReadPort;
import kwh.cofshop.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final MemberReadPort memberReadPort;
    private final CartItemRepository cartItemRepository;

    private final CartMapper cartMapper;

    // 장바구니가 없다면 생성을 해준다.
    @Transactional
    public CartResponseDto createCart(Long memberId) {
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseGet(() -> {
                    Member member = memberReadPort.getById(memberId);
                    return cartRepository.save(new Cart(member));
                });
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
