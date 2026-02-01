package kwh.cofshop.cart.service;

import kwh.cofshop.cart.domain.Cart;
import kwh.cofshop.cart.domain.CartItem;
import kwh.cofshop.cart.dto.request.CartItemRequestDto;
import kwh.cofshop.cart.dto.response.CartItemResponseDto;
import kwh.cofshop.cart.mapper.CartItemMapper;
import kwh.cofshop.cart.repository.CartItemRepository;
import kwh.cofshop.cart.repository.CartRepository;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.repository.ItemOptionRepository;
import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartItemServiceTest {

    @Mock
    private CartItemMapper cartItemMapper;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemOptionRepository itemOptionRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private CartItemService cartItemService;

    @Test
    @DisplayName("장바구니 단건 추가: 장바구니 없음")
    void addCartItem_cartNotFound() {
        when(cartRepository.findByMemberId(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartItemService.addCartItem(new CartItemRequestDto(), 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("장바구니 단건 추가: 상품 없음")
    void addCartItem_itemNotFound() {
        Cart cart = createCartWithId(1L);
        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.of(cart));
        when(itemRepository.findById(10L)).thenReturn(Optional.empty());

        CartItemRequestDto requestDto = new CartItemRequestDto();
        requestDto.setItemId(10L);
        requestDto.setOptionId(100L);
        requestDto.setQuantity(1);

        assertThatThrownBy(() -> cartItemService.addCartItem(requestDto, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("장바구니 단건 추가: 옵션 없음")
    void addCartItem_optionNotFound() {
        Cart cart = createCartWithId(1L);
        Item item = createItem(1000);

        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.of(cart));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(itemOptionRepository.findById(100L)).thenReturn(Optional.empty());

        CartItemRequestDto requestDto = new CartItemRequestDto();
        requestDto.setItemId(10L);
        requestDto.setOptionId(100L);
        requestDto.setQuantity(1);

        assertThatThrownBy(() -> cartItemService.addCartItem(requestDto, 1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("장바구니 단건 추가: 기존 아이템 수량 증가")
    void addCartItem_existingItem() {
        Cart cart = createCartWithId(1L);
        Item item = createItem(1000);
        ItemOption option = createOption(item, 200, 100);
        ReflectionTestUtils.setField(option, "id", 100L);

        CartItem existing = CartItem.builder()
                .cart(cart)
                .item(item)
                .itemOption(option)
                .quantity(1)
                .build();

        CartItemResponseDto responseDto = new CartItemResponseDto();

        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.of(cart));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(itemOptionRepository.findById(100L)).thenReturn(Optional.of(option));
        when(cartItemRepository.findByCartIdAndItemOptionId(1L, 100L)).thenReturn(Optional.of(existing));
        when(cartItemMapper.toResponseDto(existing)).thenReturn(responseDto);

        CartItemRequestDto requestDto = new CartItemRequestDto();
        requestDto.setItemId(10L);
        requestDto.setOptionId(100L);
        requestDto.setQuantity(2);

        CartItemResponseDto result = cartItemService.addCartItem(requestDto, 1L);

        assertThat(result).isSameAs(responseDto);
        assertThat(existing.getQuantity()).isEqualTo(3);
        verify(cartItemRepository, never()).save(existing);
    }

    @Test
    @DisplayName("장바구니 단건 추가: 신규 아이템 저장")
    void addCartItem_newItem() {
        Cart cart = createCartWithId(1L);
        Item item = createItem(1000);
        ItemOption option = createOption(item, 200, 100);
        ReflectionTestUtils.setField(option, "id", 100L);

        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.of(cart));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(itemOptionRepository.findById(100L)).thenReturn(Optional.of(option));
        when(cartItemRepository.findByCartIdAndItemOptionId(1L, 100L)).thenReturn(Optional.empty());
        when(cartItemMapper.toResponseDto(org.mockito.ArgumentMatchers.any(CartItem.class)))
                .thenReturn(new CartItemResponseDto());

        CartItemRequestDto requestDto = new CartItemRequestDto();
        requestDto.setItemId(10L);
        requestDto.setOptionId(100L);
        requestDto.setQuantity(2);

        cartItemService.addCartItem(requestDto, 1L);

        verify(cartItemRepository).save(org.mockito.ArgumentMatchers.any(CartItem.class));
        assertThat(cart.getCartItems()).hasSize(1);
    }

    @Test
    @DisplayName("장바구니 목록 추가")
    void addCartItemList() {
        Cart cart = createCartWithId(1L);
        Item item = createItem(1000);
        ItemOption option = createOption(item, 200, 100);
        ReflectionTestUtils.setField(option, "id", 100L);

        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.of(cart));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(itemOptionRepository.findById(100L)).thenReturn(Optional.of(option));
        when(cartItemRepository.findByCartIdAndItemOptionId(1L, 100L)).thenReturn(Optional.empty());
        when(cartItemMapper.toResponseDto(org.mockito.ArgumentMatchers.any(CartItem.class)))
                .thenReturn(new CartItemResponseDto());

        CartItemRequestDto requestDto = new CartItemRequestDto();
        requestDto.setItemId(10L);
        requestDto.setOptionId(100L);
        requestDto.setQuantity(1);

        List<CartItemResponseDto> results = cartItemService.addCartItemList(List.of(requestDto), 1L);

        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("장바구니 아이템 삭제: 장바구니 없음")
    void deleteCartItemByOptionId_cartNotFound() {
        when(cartRepository.findByMemberId(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartItemService.deleteCartItemByOptionId(1L, 100L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("장바구니 아이템 삭제: 아이템 없음")
    void deleteCartItemByOptionId_itemNotFound() {
        Cart cart = createCartWithId(1L);
        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndItemOptionId(1L, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartItemService.deleteCartItemByOptionId(1L, 100L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("장바구니 아이템 삭제: 성공")
    void deleteCartItemByOptionId_success() {
        Cart cart = createCartWithId(1L);
        CartItem cartItem = CartItem.builder().cart(cart).quantity(1).build();

        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndItemOptionId(1L, 100L)).thenReturn(Optional.of(cartItem));

        cartItemService.deleteCartItemByOptionId(1L, 100L);

        verify(cartItemRepository).delete(cartItem);
    }

    @Test
    @DisplayName("장바구니 수량 변경: 성공")
    void changeQuantity_success() {
        Cart cart = createCartWithId(1L);
        Item item = createItem(1000);
        ItemOption option = createOption(item, 200, 100);
        ReflectionTestUtils.setField(option, "id", 100L);

        CartItem cartItem = CartItem.builder()
                .cart(cart)
                .item(item)
                .itemOption(option)
                .quantity(1)
                .build();

        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndItemOptionId(1L, 100L)).thenReturn(Optional.of(cartItem));

        CartItemRequestDto requestDto = new CartItemRequestDto();
        requestDto.setOptionId(100L);
        requestDto.setQuantity(5);

        cartItemService.changeQuantity(1L, requestDto);

        assertThat(cartItem.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("장바구니 목록 조회")
    void getCartItemsByMemberId() {
        Cart cart = createCartWithId(1L);
        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findCartItemsByMemberId(1L)).thenReturn(List.of(new CartItemResponseDto()));

        List<CartItemResponseDto> results = cartItemService.getCartItemsByMemberId(1L);

        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("장바구니 전체 삭제")
    void deleteCartItemAll() {
        Cart cart = createCartWithId(1L);
        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.of(cart));

        cartItemService.deleteCartItemAll(1L);

        verify(cartItemRepository).deleteAllByCartId(1L);
    }

    @Test
    @DisplayName("장바구니 합계 계산")
    void calculateTotalPrice() {
        Cart cart = createCartWithId(1L);
        Item item1 = createItem(1000);
        ItemOption option1 = createOption(item1, 200, 100);
        CartItem cartItem1 = CartItem.builder()
                .item(item1)
                .itemOption(option1)
                .quantity(2)
                .cart(cart)
                .build();
        cart.addCartItem(cartItem1);

        Item item2 = createItem(500);
        ItemOption option2 = createOption(item2, 0, 50);
        CartItem cartItem2 = CartItem.builder()
                .item(item2)
                .itemOption(option2)
                .quantity(1)
                .cart(cart)
                .build();
        cart.addCartItem(cartItem2);

        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.of(cart));

        int totalPrice = cartItemService.calculateTotalPrice(1L);

        assertThat(totalPrice).isEqualTo(2900);
    }

    private Cart createCartWithId(Long id) {
        Member member = Member.builder()
                .id(1L)
                .email("user@example.com")
                .memberName("사용자")
                .memberPwd("pw")
                .tel("01012341234")
                .build();
        Cart cart = new Cart(member);
        ReflectionTestUtils.setField(cart, "id", id);
        return cart;
    }

    private Item createItem(int price) {
        return Item.builder()
                .itemName("커피")
                .price(price)
                .deliveryFee(0)
                .origin("브라질")
                .itemLimit(10)
                .seller(Member.builder()
                        .id(2L)
                        .email("seller@example.com")
                        .memberName("판매자")
                        .memberPwd("pw")
                        .tel("01099998888")
                        .build())
                .build();
    }

    private ItemOption createOption(Item item, int additionalPrice, int stock) {
        return ItemOption.builder()
                .item(item)
                .additionalPrice(additionalPrice)
                .stock(stock)
                .build();
    }
}