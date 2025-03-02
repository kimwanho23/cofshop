package kwh.cofshop.cart.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import kwh.cofshop.cart.dto.request.CartItemRequestDto;
import kwh.cofshop.cart.dto.response.CartItemResponseDto;
import kwh.cofshop.cart.dto.response.CartResponseDto;
import kwh.cofshop.cart.service.CartItemService;
import kwh.cofshop.cart.service.CartService;
import kwh.cofshop.config.argumentResolver.LoginMember;
import kwh.cofshop.global.response.ApiResponse;
import kwh.cofshop.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
public class CartController {

    private final CartService cartService;

    private final CartItemService cartItemService;

    // 장바구니 추가
    @PostMapping(value = "/addCart")
    public ResponseEntity<ApiResponse<List<CartItemResponseDto>>> addCart(
            @LoginMember CustomUserDetails customUserDetails,
            @RequestBody List<CartItemRequestDto> cartItemRequestDto) {
        List<CartItemResponseDto> cartItemResponseDto =
                cartItemService.addCartItem(cartItemRequestDto, customUserDetails.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Created(cartItemResponseDto));
    }

    // 멤버의 장바구니 목록 조회
    @GetMapping(value = "/getCart")
    public ResponseEntity<ApiResponse<CartResponseDto>> getMemberCartItem(
            @LoginMember CustomUserDetails customUserDetails){
        CartResponseDto memberCartItems = cartService.getMemberCartItems(customUserDetails.getId());

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.Created(memberCartItems));
    }

    // 장바구니 아이템 개별 삭제
    @DeleteMapping("/cart/{cartId}/item/{itemId}/option/{optionId}")
    public ResponseEntity<Void> deleteCartItem(
            @PathVariable Long cartId,
            @PathVariable Long itemId,
            @PathVariable Long optionId,
            @LoginMember CustomUserDetails customUserDetails) {

        cartItemService.deleteCartItem(customUserDetails.getId(), cartId, itemId, optionId);
        return ResponseEntity.noContent().build();
    }


    // 장바구니 아이템 일괄 삭제
    @DeleteMapping("/{cartId}/items")
    public ResponseEntity<Void> deleteAllCartItems(@PathVariable Long cartId, @LoginMember CustomUserDetails customUserDetails) {
        cartItemService.deleteCartItemAll(cartId);
        return ResponseEntity.noContent().build();
    }


}
