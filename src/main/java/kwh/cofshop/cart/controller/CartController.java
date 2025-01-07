package kwh.cofshop.cart.controller;

import kwh.cofshop.cart.dto.request.CartItemRequestDto;
import kwh.cofshop.cart.dto.response.CartItemResponseDto;
import kwh.cofshop.cart.dto.response.CartResponseDto;
import kwh.cofshop.cart.service.CartItemService;
import kwh.cofshop.cart.service.CartService;
import kwh.cofshop.config.argumentResolver.LoginMember;
import kwh.cofshop.global.response.ApiResponse;
import kwh.cofshop.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
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
            @LoginMember Member member){
        CartResponseDto memberCartItems = cartService.getMemberCartItems(member);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.Created(memberCartItems));
    }

    @DeleteMapping("/item/{cartItemId}")
    public ResponseEntity<Void> deleteCartItem(@PathVariable Long cartItemId) {
        cartService.deleteCartItem(cartItemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{cartId}/items")
    public ResponseEntity<Void> deleteAllCartItems(@PathVariable Long cartId) {
        cartService.deleteCartItemAll(cartId);
        return ResponseEntity.noContent().build();
    }


}
