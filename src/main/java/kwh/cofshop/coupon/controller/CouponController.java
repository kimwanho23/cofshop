package kwh.cofshop.coupon.controller;

import jakarta.validation.Valid;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.dto.request.CouponRequestDto;
import kwh.cofshop.coupon.dto.response.CouponResponseDto;
import kwh.cofshop.coupon.service.CouponService;
import kwh.cofshop.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupon")
public class CouponController {

    private final CouponService couponService;

    ///@GET
    // 쿠폰 단건 조회
    @GetMapping("/{couponId}")
    public ResponseEntity<ApiResponse<CouponResponseDto>> getCouponById(@PathVariable Long couponId) {
        CouponResponseDto coupon = couponService.getCouponById(couponId);
        return ResponseEntity.ok(ApiResponse.OK(coupon));
    }

    // 전체 쿠폰 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<CouponResponseDto>>> getAllCoupons() {
        List<CouponResponseDto> coupons = couponService.getAllCoupons();
        return ResponseEntity.ok(ApiResponse.OK(coupons));
    }

    ///@POST
    // 쿠폰 생성
    @PostMapping
    public ResponseEntity<ApiResponse<CouponResponseDto>> createCoupon(
            @RequestBody @Valid CouponRequestDto couponRequestDto) {
        CouponResponseDto createdCoupon = couponService.createCoupon(couponRequestDto);
        return ResponseEntity.created(URI.create("/api/coupon"))
                .body(ApiResponse.Created(createdCoupon));
    }

    ///@PUT, PATCH
    // 쿠폰 상태 변경
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PatchMapping("/{couponId}/state")
    public ResponseEntity<ApiResponse<Void>> updateCouponState(
            @PathVariable Long couponId,
            @RequestParam CouponState newState) {
        couponService.updateCouponState(couponId, newState);
        return ResponseEntity.ok().build();
    }


    // 쿠폰 발급 취소 (상태를 CANCELLED로 변경)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PatchMapping("/{couponId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelCoupon(@PathVariable Long couponId) {
        couponService.cancelCoupon(couponId);
        return ResponseEntity.ok().build();
    }

    // 기간 조회해서 쿠폰 만료 (관리자가 직접)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/expire")
    public ResponseEntity<ApiResponse<Void>> expireCoupons(@RequestParam LocalDate date) {
        couponService.expireCoupons(date);
        return ResponseEntity.ok().build();
    }

    ///@DELETE
}
