package kwh.cofshop.cart.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import kwh.cofshop.cart.dto.response.CartResponseDto;
import kwh.cofshop.cart.service.CartService;
import kwh.cofshop.config.argumentResolver.LoginMember;
import kwh.cofshop.global.response.ApiResponse;
import kwh.cofshop.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/carts")
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
public class CartController {

    private final CartService cartService;

    // 장바구니의 존재 여부 확인
    @GetMapping("/me/exists")
    public ResponseEntity<ApiResponse<Boolean>> checkCartExists(@LoginMember CustomUserDetails user) {
        boolean exists = cartService.checkCartExistByMemberId(user.getId());
        return ResponseEntity.ok(ApiResponse.OK(exists));
    }

    // 장바구니 생성
    // Case 1. 최초 회원 가입 시 Listener를 통해서 장바구니 생성
    // Case 2. 장바구니 확인 시, 장바구니가 없다면 생성

    @PostMapping("/me")
    public ResponseEntity<ApiResponse<CartResponseDto>> createCart(@LoginMember CustomUserDetails user) {
        CartResponseDto cart = cartService.createCart(user.getId());
        return ResponseEntity.created(URI.create("/api/carts/" + cart.getId()))
                .body(ApiResponse.Created(cart));
    }

    // 장바구니 삭제
    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> deleteCart(@PathVariable Long memberId) {
        cartService.deleteByMemberId(memberId);
        return ResponseEntity.noContent().build();
    }
}
