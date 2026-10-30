package kwh.cofshop.cart.service;


import kwh.cofshop.cart.domain.Cart;
import kwh.cofshop.cart.domain.CartItem;
import kwh.cofshop.cart.dto.request.CartItemRequestDto;
import kwh.cofshop.cart.dto.response.CartItemResponseDto;
import kwh.cofshop.cart.mapper.CartItemMapper;
import kwh.cofshop.cart.repository.CartItemRepository;
import kwh.cofshop.cart.repository.CartRepository;
import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.item.api.ItemReadPort;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartItemService {
    private final CartItemMapper cartItemMapper;
    private final ItemReadPort itemReadPort;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    // 장바구니에 상품 단일 등록
    @Transactional
    public CartItemResponseDto addCartItem(CartItemRequestDto cartItemRequestDto, Long memberId) {
        validateQuantity(cartItemRequestDto.getQuantity());

        Cart cart = cartRepository.findByMemberIdWithLock(memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.CART_NOT_FOUND));

        Item item = itemReadPort.findItemById(cartItemRequestDto.getItemId())
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ITEM_NOT_FOUND));

        ItemOption itemOption = itemReadPort.findItemOptionById(cartItemRequestDto.getOptionId())
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ITEM_OPTION_NOT_FOUND));

        if (!itemOption.getItem().getId().equals(item.getId())) {
            throw new BusinessException(BusinessErrorCode.ITEM_OPTION_NOT_FOUND);
        }

        Optional<CartItem> optionalCartItem = cartItemRepository
                .findByCartIdAndItemOptionIdWithLock(cart.getId(), itemOption.getId());

        CartItem cartItem;
        if (optionalCartItem.isPresent()) {
            cartItem = optionalCartItem.get();
            int updatedQuantity = safeAddQuantity(cartItem.getQuantity(), cartItemRequestDto.getQuantity());
            cartItem.changeQuantity(updatedQuantity);
        } else {
            cartItem = CartItem.builder()
                    .quantity(cartItemRequestDto.getQuantity())
                    .itemOption(itemOption)
                    .item(item)
                    .cart(cart)
                    .build();
            cart.addCartItem(cartItem);
            cartItemRepository.save(cartItem);
        }
        return cartItemMapper.toResponseDto(cartItem);
    }

    // 장바구니 리스트 등록
    @Transactional
    public List<CartItemResponseDto> addCartItemList(List<CartItemRequestDto> requestList, Long memberId) {
        return requestList.stream()
                .map(request -> addCartItem(request, memberId))
                .collect(Collectors.toList());
    }

    // 장바구니 요소 삭제
    @Transactional
    public void deleteCartItemByOptionId(Long memberId, Long itemOptionId) {
        // 현재 로그인한 사용자의 장바구니 가져오기
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.CART_NOT_FOUND));

        CartItem cartItem = cartItemRepository.findByCartIdAndItemOptionId(cart.getId(), itemOptionId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ITEM_OPTION_NOT_FOUND));

        cartItemRepository.delete(cartItem);
    }

    // 장바구니 아이템 수량 변경
    @Transactional
    public void changeQuantity(Long memberId, CartItemRequestDto cartItemRequestDto) {
        validateQuantity(cartItemRequestDto.getQuantity());

        // 현재 로그인한 사용자의 장바구니 가져오기
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.CART_NOT_FOUND));

        CartItem cartItem = cartItemRepository.findByCartIdAndItemOptionId(cart.getId(), cartItemRequestDto.getOptionId())
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ITEM_OPTION_NOT_FOUND));

        cartItem.changeQuantity(cartItemRequestDto.getQuantity());
    }

    // 회원 장바구니 조회 ( 장바구니에 있는 물건 )
    @Transactional(readOnly = true)
    public List<CartItemResponseDto> getCartItemsByMemberId(Long memberId) {
        if (!cartRepository.existsByMemberId(memberId)) {
            throw new BusinessException(BusinessErrorCode.CART_NOT_FOUND);
        }
        return cartItemRepository.findCartItemsByMemberId(memberId);
    }


    // 장바구니 전체 삭제
    @Transactional
    public void deleteCartItemAll(Long memberId) {
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.CART_NOT_FOUND));
        cartItemRepository.deleteAllByCartId(cart.getId());
    }


    // 장바구니 금액 계산
    @Transactional(readOnly = true)
    public int calculateTotalPrice(Long memberId) {
        if (!cartRepository.existsByMemberId(memberId)) {
            throw new BusinessException(BusinessErrorCode.CART_NOT_FOUND);
        }

        Long totalPrice = cartItemRepository.sumTotalPriceByMemberId(memberId);
        if (totalPrice == null) {
            return 0;
        }
        if (totalPrice > Integer.MAX_VALUE || totalPrice < 0) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
        return totalPrice.intValue();
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
    }

    private int safeAddQuantity(int currentQuantity, int addQuantity) {
        long updated = (long) currentQuantity + addQuantity;
        if (updated > Integer.MAX_VALUE) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
        return (int) updated;
    }
}
