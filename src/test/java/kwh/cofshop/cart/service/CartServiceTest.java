package kwh.cofshop.cart.service;

import kwh.cofshop.cart.domain.Cart;
import kwh.cofshop.cart.dto.response.CartResponseDto;
import kwh.cofshop.cart.mapper.CartMapper;
import kwh.cofshop.cart.repository.CartItemRepository;
import kwh.cofshop.cart.repository.CartRepository;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
    private MemberRepository memberRepository;

    @Mock
    private CartMapper cartMapper;

    @InjectMocks
    private CartService cartService;

    @Test
    @DisplayName("장바구니 생성: 회원 없음")
    void createCart_memberNotFound() {
        when(memberRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.createCart(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("장바구니 생성: 기존 장바구니 반환")
    void createCart_existingCart() {
        Member member = Member.builder()
                .id(1L)
                .email("user@example.com")
                .memberName("사용자")
                .memberPwd("pw")
                .tel("01012341234")
                .build();
        Cart existingCart = member.getCart();
        CartResponseDto responseDto = new CartResponseDto();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(cartMapper.toResponseDto(existingCart)).thenReturn(responseDto);

        CartResponseDto result = cartService.createCart(1L);

        assertThat(result).isSameAs(responseDto);
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("장바구니 생성: 신규 생성")
    void createCart_newCart() {
        Member member = Member.builder()
                .id(1L)
                .email("user@example.com")
                .memberName("사용자")
                .memberPwd("pw")
                .tel("01012341234")
                .build();
        ReflectionTestUtils.setField(member, "cart", null);

        Cart savedCart = new Cart(member);
        CartResponseDto responseDto = new CartResponseDto();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(cartRepository.save(any(Cart.class))).thenReturn(savedCart);
        when(cartMapper.toResponseDto(savedCart)).thenReturn(responseDto);

        CartResponseDto result = cartService.createCart(1L);

        assertThat(result).isSameAs(responseDto);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("장바구니 존재 여부 조회")
    void checkCartExistByMemberId() {
        when(cartRepository.existsByMemberId(1L)).thenReturn(true);

        boolean exists = cartService.checkCartExistByMemberId(1L);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("장바구니 삭제: 대상 없음")
    void deleteByMemberId_notFound() {
        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.deleteByMemberId(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("장바구니 삭제: 성공")
    void deleteByMemberId_success() {
        Member member = Member.builder()
                .id(1L)
                .email("user@example.com")
                .memberName("사용자")
                .memberPwd("pw")
                .tel("01012341234")
                .build();
        Cart cart = new Cart(member);

        when(cartRepository.findByMemberId(1L)).thenReturn(Optional.of(cart));

        cartService.deleteByMemberId(1L);

        verify(cartRepository).delete(cart);
    }
}