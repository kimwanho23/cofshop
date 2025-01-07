package kwh.cofshop.cart.service;


import kwh.cofshop.cart.domain.Cart;
import kwh.cofshop.cart.domain.CartItem;
import kwh.cofshop.cart.dto.request.CartItemRequestDto;
import kwh.cofshop.cart.dto.response.CartItemResponseDto;
import kwh.cofshop.cart.mapper.CartItemMapper;
import kwh.cofshop.cart.repository.CartItemRepository;
import kwh.cofshop.cart.repository.CartRepository;
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

    @Transactional
    public List<CartItemResponseDto> addCartItem(List<CartItemRequestDto> cartItemRequestDto, Member member) {

        Cart cart = cartRepository.findByMember(member).orElseThrow();

        List<CartItemResponseDto> responseDto = new ArrayList<>();

        // 중복을 제거하고 수량을 합산
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

        // 2. 병합된 요청 처리
        for (CartItemRequestDto requestDto : mergedCartItems.values()) {
            Item item = itemRepository.findById(requestDto.getItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Item not found: " + requestDto.getItemId()));

            ItemOption itemOption = itemOptionRepository.findById(requestDto.getOptionId())
                    .orElseThrow(() -> new IllegalArgumentException("ItemOption not found: " + requestDto.getOptionId()));

            // 3. 기존 장바구니 아이템 조회
            CartItem cartItem = cart.getCartItems().stream()
                    .filter(ci -> Objects.equals(ci.getItem(), item) && Objects.equals(ci.getItemOption(), itemOption))
                    .findFirst()
                    .orElseGet(() -> {
                        CartItem newCartItem = CartItem.builder()
                                .quantity(0)  // 수량을 초기화하고 아래에서 추가
                                .itemOption(itemOption)
                                .cart(cart)
                                .item(item)
                                .build();
                        cart.addCartItem(newCartItem);
                        return newCartItem;
                    });

            // 4. 수량 추가
            cartItem.addQuantity(requestDto.getQuantity());

            cartItemRepository.save(cartItem);
            responseDto.add(cartItemMapper.toResponseDto(cartItem));
        }
        return responseDto;
    }


}
