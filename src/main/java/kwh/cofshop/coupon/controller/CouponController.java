package kwh.cofshop.coupon.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.dto.request.CouponRequestDto;
import kwh.cofshop.coupon.dto.response.CouponResponseDto;
import kwh.cofshop.coupon.service.CouponService;
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

    /// @GET
    // 쿠폰 단건 조회
    @Operation(summary = "쿠폰 단건 조회", description = "한 쿠폰의 정보를 조회합니다.")
    @GetMapping("/{couponId}")
    public CouponResponseDto getCouponById(@PathVariable Long couponId) {
        CouponResponseDto coupon = couponService.getCouponById(couponId);
        return coupon;
    }

    // 전체 쿠폰 조회
    @Operation(summary = "쿠폰 전체 조회", description = "현재 생성되어있는 전체 쿠폰을 조회합니다.")
    @GetMapping
    public List<CouponResponseDto> getAllCoupons() {
        List<CouponResponseDto> coupons = couponService.getAllCoupons();
        return coupons;
    }

    /// @POST
    // 쿠폰 생성
    @Operation(summary = "쿠폰 생성", description = "쿠폰을 생성합니다.")
    @PostMapping
    public ResponseEntity<Long> createCoupon(
            @RequestBody @Valid CouponRequestDto couponRequestDto) {
        Long coupon = couponService.createCoupon(couponRequestDto);
        return ResponseEntity.created(URI.create("/api/coupon"))
                .body(coupon);
    }

    /// @PUT, PATCH
    // 쿠폰 상태 변경
    @Operation(summary = "쿠폰 상태 변경", description = "쿠폰의 사용가능 여부를 변경합니다.")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PatchMapping("/{couponId}/state")
    public ResponseEntity<Void> updateCouponState(
            @PathVariable Long couponId,
            @RequestParam CouponState newState) {
        couponService.updateCouponState(couponId, newState);
        return ResponseEntity.noContent().build();
    }


    // 쿠폰 발급 취소 (상태를 CANCELLED로 변경)
    @Operation(summary = "쿠폰 발급 취소", description = "쿠폰을 발급 불가능 상태로 변경합니다.")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PatchMapping("/{couponId}/cancel")
    public ResponseEntity<Void> cancelCoupon(@PathVariable Long couponId) {
        couponService.cancelCoupon(couponId);
        return ResponseEntity.noContent().build();
    }

    // 기간 조회해서 쿠폰 만료 (관리자가 직접)
    @Operation(summary = "쿠폰 만료", description = "관리자 전용입니다, 직접 쿠폰의 상태를 만료시킵니다.")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/expire")
    public ResponseEntity<Void> expireCoupons(@RequestParam LocalDate date) {
        couponService.expireCoupons(date);
        return ResponseEntity.noContent().build();
    }

    ///@DELETE
}
