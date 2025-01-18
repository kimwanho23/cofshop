package kwh.cofshop.cart.controller;

import kwh.cofshop.cart.dto.request.CartItemRequestDto;
import kwh.cofshop.cart.dto.response.CartItemResponseDto;
import kwh.cofshop.cart.dto.response.CartResponseDto;
import kwh.cofshop.cart.service.CartItemService;
import kwh.cofshop.cart.service.CartService;
import kwh.cofshop.config.argumentResolver.LoginMember;
import kwh.cofshop.global.response.ApiResponse;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.Role;
import kwh.cofshop.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
@Slf4j
public class CartController {

    private final CartService cartService;

    private final CartItemService cartItemService;

    @PostMapping(value = "/addCart")
    public ResponseEntity<ApiResponse<List<CartItemResponseDto>>> addCart(
            @LoginMember Member member,
            @RequestBody List<CartItemRequestDto> cartItemRequestDto) {

        List<CartItemResponseDto> cartItemResponseDto = cartItemService.addCartItem(cartItemRequestDto, member);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Created(cartItemResponseDto));
    }

    @GetMapping(value = "/getCart")
    public ResponseEntity<ApiResponse<CartResponseDto>> getMemberCartItem(
            @AuthenticationPrincipal CustomUserDetails userDetails){
        CartResponseDto memberCartItems = cartService.getMemberCartItems(userDetails.getEmail());

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.Created(memberCartItems));
    }

    @DeleteMapping("/item/{cartItemId}")
    public ResponseEntity<Void> deleteCartItem(@PathVariable Long cartItemId, @LoginMember Member member) {
        cartService.deleteCartItem(cartItemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{cartId}/items")
    public ResponseEntity<Void> deleteAllCartItems(@PathVariable Long cartId, @LoginMember Member member) {
        cartService.deleteCartItemAll(cartId);
        return ResponseEntity.noContent().build();
    }


}
