package kwh.cofshop.cart.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import kwh.cofshop.argumentResolver.LoginMember;
import kwh.cofshop.cart.dto.response.CartResponseDto;
import kwh.cofshop.cart.service.CartService;
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

    // 장바구니 존재 확인
    @Operation(summary = "장바구니 존재 확인", description = "멤버의 장바구니가 존재하는지 확인합니다.")
    @GetMapping("/me/exists")
    public Boolean checkCartExists(@LoginMember Long memberId) {
        boolean exists = cartService.checkCartExistByMemberId(memberId);
        return exists;
    }

    // 장바구니 생성
    // Case 1. 최초 회원 가입시 Listener를 통해서 장바구니 생성
    // Case 2. 장바구니 확인 후 장바구니가 없다면 생성

    @Operation(summary = "장바구니 생성", description = "장바구니를 생성합니다.")
    @PostMapping("/me")
    public ResponseEntity<CartResponseDto> createCart(@LoginMember Long memberId) {
        CartResponseDto cart = cartService.createCart(memberId);
        return ResponseEntity.created(URI.create("/api/carts/" + cart.getId()))
                .body(cart);
    }

    // 장바구니 삭제
    @Operation(summary = "장바구니 삭제", description = "장바구니를 삭제합니다. 회원 탈퇴 시 적용합니다.")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCart(@LoginMember Long memberId) {
        cartService.deleteByMemberId(memberId);
        return ResponseEntity.noContent().build();
    }
}
