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

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/carts")
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
public class CartController {

    private final CartService cartService;

    private final CartItemService cartItemService;

    //////////// @GET
    // 자신의 장바구니 목록 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CartResponseDto>> getMyCart(
            @LoginMember CustomUserDetails user) {

        CartResponseDto cart = cartService.getMemberCartItems(user.getId());
        return ResponseEntity.ok(ApiResponse.OK(cart));
    }

    //////////// @POST

    // 장바구니 추가
    @PostMapping("/me/items")
    public ResponseEntity<ApiResponse<List<CartItemResponseDto>>> addCartItem(
            @LoginMember CustomUserDetails user,
            @RequestBody List<CartItemRequestDto> request) {

        List<CartItemResponseDto> response = cartItemService.addCartItem(request, user.getId());
        return ResponseEntity.created(URI.create("/api/carts/me/items"))
                .body(ApiResponse.Created(response));
    }

    //////////// @PUT, PATCH


    //////////// @DELETE
    // 장바구니 아이템 개별 삭제
    @DeleteMapping("/me/items/{itemOptionId}")
    public ResponseEntity<Void> deleteCartItem(
            @PathVariable Long itemOptionId,
            @LoginMember CustomUserDetails user) {

        cartItemService.deleteCartItemByOptionId(user.getId(), itemOptionId);
        return ResponseEntity.noContent().build();
    }


    // 장바구니 아이템 일괄 삭제
    @DeleteMapping("/me/items")
    public ResponseEntity<Void> deleteAllCartItems(@LoginMember CustomUserDetails user) {
        cartItemService.deleteCartItemAll(user.getId());
        return ResponseEntity.noContent().build();
    }
}
