package kwh.cofshop.cart.service;


import kwh.cofshop.cart.domain.Cart;
import kwh.cofshop.cart.domain.CartItem;
import kwh.cofshop.cart.dto.request.CartItemRequestDto;
import kwh.cofshop.cart.dto.response.CartItemResponseDto;
import kwh.cofshop.cart.mapper.CartItemMapper;
import kwh.cofshop.cart.repository.CartItemRepository;
import kwh.cofshop.cart.repository.CartRepository;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.repository.ItemOptionRepository;
import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CartItemService {


    private final CartItemMapper cartItemMapper;

    private final ItemRepository itemRepository;
    private final ItemOptionRepository itemOptionRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;


    // 장바구니에 상품 추가
    @Transactional
    public List<CartItemResponseDto> addCartItem(List<CartItemRequestDto> cartItemRequestDto, Member member) {

        Cart cart = cartRepository.findByMember(member).orElseThrow();

        List<CartItemResponseDto> responseDto = new ArrayList<>();

        // 중복을 제거하고 수량을 합산하기 위해서 Map 선언
        Map<String, CartItemRequestDto> mergedCartItems = new HashMap<>();

        for (CartItemRequestDto requestDto : cartItemRequestDto) {
            String key = requestDto.getItemId() + "-" + requestDto.getOptionId();

            // 이미 존재하는 경우 수량 증가
            if (mergedCartItems.containsKey(key)) {
                CartItemRequestDto existingRequestDto = mergedCartItems.get(key);
                existingRequestDto.setQuantity(existingRequestDto.getQuantity() + requestDto.getQuantity());
            } else {
                // 존재하지 않는 경우 새로 추가
                mergedCartItems.put(key, requestDto);
            }
        }

        // 병합된 요청 처리
        for (CartItemRequestDto requestDto : mergedCartItems.values()) {
            Item item = itemRepository.findById(requestDto.getItemId())
                    .orElseThrow(() -> new BusinessException(BusinessErrorCode.MEMBER_NOT_FOUND)); // 회원을 찾기 못한 경우

            ItemOption itemOption = itemOptionRepository.findById(requestDto.getOptionId())
                    .orElseThrow(() -> new BusinessException(BusinessErrorCode.ITEM_NOT_FOUND)); // 아이템을 찾지 못한 경우

            // 기존 장바구니 아이템 조회
            CartItem cartItem = cart.getCartItems().stream()
                    .filter(ci -> Objects.equals(ci.getItem(), item) && Objects.equals(ci.getItemOption(), itemOption))
                    .findFirst()
                    .orElseGet(() -> { // 중복을 제거하는 코드
                        CartItem newCartItem = CartItem.builder()
                                .quantity(0) // Quantity를 0으로 재설정
                                .itemOption(itemOption)
                                .cart(cart)
                                .item(item)
                                .build();
                        cart.addCartItem(newCartItem); // addCartItem의 수량으로 맞춘다. (중복이 제거됨)
                        return newCartItem;
                    });

            // 수량 추가
            cartItem.addQuantity(requestDto.getQuantity());

            //저장 후 반환
            cartItemRepository.save(cartItem);
            responseDto.add(cartItemMapper.toResponseDto(cartItem));
        }
        return responseDto;
    }


}
