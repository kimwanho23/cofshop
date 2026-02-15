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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private MemberReadPort memberReadPort;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CartMapper cartMapper;

    @InjectMocks
    private CartService cartService;

    @Test
    @DisplayName("createCart: member not found")
    void createCart_memberNotFound() {
        when(cartRepository.findByMemberId(anyLong())).thenReturn(Optional.empty());
        when(memberReadPort.getById(anyLong())).thenThrow(new BusinessException(BusinessErrorCode.MEMBER_NOT_FOUND));

        assertThatThrownBy(() -> cartService.createCart(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("createCart: return existing cart")
    void createCart_existingCart() {
        Member member = member(1L);
        Cart existingCart = new Cart(member);
        CartResponseDto responseDto = new CartResponseDto();

        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.of(existingCart));
        when(cartMapper.toResponseDto(existingCart)).thenReturn(responseDto);

        CartResponseDto result = cartService.createCart(1L);

        assertThat(result).isSameAs(responseDto);
        verify(cartRepository, never()).save(any());
        verify(memberReadPort, never()).getById(anyLong());
    }

    @Test
    @DisplayName("createCart: create new cart")
    void createCart_newCart() {
        Member member = member(1L);
        Cart savedCart = new Cart(member);
        CartResponseDto responseDto = new CartResponseDto();

        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.empty());
        when(memberReadPort.getById(1L)).thenReturn(member);
        when(cartRepository.save(any(Cart.class))).thenReturn(savedCart);
        when(cartMapper.toResponseDto(savedCart)).thenReturn(responseDto);

        CartResponseDto result = cartService.createCart(1L);

        assertThat(result).isSameAs(responseDto);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("checkCartExistByMemberId")
    void checkCartExistByMemberId() {
        when(cartRepository.existsByMemberId(1L)).thenReturn(true);

        boolean exists = cartService.checkCartExistByMemberId(1L);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("deleteByMemberId: cart not found")
    void deleteByMemberId_notFound() {
        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.deleteByMemberId(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("deleteByMemberId: success")
    void deleteByMemberId_success() {
        Cart cart = new Cart(member(1L));

        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.of(cart));

        cartService.deleteByMemberId(1L);

        verify(cartRepository).delete(cart);
    }

    private Member member(Long id) {
        return Member.builder()
                .id(id)
                .email("user@example.com")
                .memberName("user")
                .memberPwd("pw")
                .tel("01012341234")
                .build();
    }
}
