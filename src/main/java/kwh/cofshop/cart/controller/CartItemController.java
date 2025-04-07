package kwh.cofshop.cart.controller;


import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import kwh.cofshop.cart.dto.request.CartItemRequestDto;
import kwh.cofshop.cart.dto.response.CartItemResponseDto;
import kwh.cofshop.cart.service.CartItemService;
import kwh.cofshop.config.argumentResolver.LoginMember;
import kwh.cofshop.global.response.ApiResponse;
import kwh.cofshop.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart-items")
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
public class CartItemController {

    private final CartItemService cartItemService;

    //////////// @GET
    // 자신의 장바구니 목록 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<CartItemResponseDto>>> getMyCartItems(
            @LoginMember CustomUserDetails user) {

        List<CartItemResponseDto> cartItemResponseDtoList = cartItemService.getCartItemsByMemberId(user.getId());
        return ResponseEntity.ok(ApiResponse.OK(cartItemResponseDtoList));
    }

    // 장바구니 총 금액 계산
    @GetMapping("/me/total-price")
    public ResponseEntity<ApiResponse<Integer>> getTotalCartPrice(@LoginMember CustomUserDetails user) {
        int totalPrice = cartItemService.calculateTotalPrice(user.getId());
        return ResponseEntity.ok(ApiResponse.OK(totalPrice));
    }

    //////////// @POST
    // 장바구니 추가
    @PostMapping("/me/items")
    public ResponseEntity<ApiResponse<CartItemResponseDto>> addCartItem(
            @LoginMember CustomUserDetails customUserDetails,
            @RequestBody CartItemRequestDto requestDto) {

        CartItemResponseDto response = cartItemService.addCartItem(requestDto, customUserDetails.getId());
        return ResponseEntity.created(URI.create("/api/carts/me/items/" + response.getOptionId()))
                .body(ApiResponse.Created(response));
    }

    // 장바구니 목록
    @PostMapping("/me/items/list")
    public ResponseEntity<ApiResponse<List<CartItemResponseDto>>> addCartItemBulk(
            @LoginMember CustomUserDetails user,
            @RequestBody List<CartItemRequestDto> requestDtoList) {

        List<CartItemResponseDto> responseDtoList = cartItemService.addCartItemList(requestDtoList, user.getId());
        return ResponseEntity.created(URI.create("/api/carts/me/items"))
                .body(ApiResponse.Created(responseDtoList));
    }

    //////////// @PUT, PATCH

    //장바구니 수량 변경
    @PatchMapping("/me/quantity")
    public ResponseEntity<Void> updateQuantity(@RequestBody CartItemRequestDto requestDto,
                                               @LoginMember CustomUserDetails customUserDetails) {
        cartItemService.changeQuantity(customUserDetails.getId(), requestDto);
        return ResponseEntity.ok().build();
    }

    //////////// @DELETE
    // 장바구니 아이템 개별 삭제
    @DeleteMapping("/me/items/{itemOptionId}")
    public ResponseEntity<Void> deleteCartItem(
            @PathVariable Long itemOptionId,
            @LoginMember CustomUserDetails customUserDetails) {

        cartItemService.deleteCartItemByOptionId(customUserDetails.getId(), itemOptionId);
        return ResponseEntity.noContent().build();
    }


    // 장바구니 아이템 일괄 삭제
    @DeleteMapping("/me/items")
    public ResponseEntity<Void> deleteAllCartItems(@LoginMember CustomUserDetails customUserDetails) {
        cartItemService.deleteCartItemAll(customUserDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
