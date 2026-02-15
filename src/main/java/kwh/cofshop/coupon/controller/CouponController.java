package kwh.cofshop.coupon.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.dto.request.CouponRequestDto;
import kwh.cofshop.coupon.dto.request.CreateCouponCommand;
import kwh.cofshop.coupon.dto.response.CouponResponseDto;
import kwh.cofshop.coupon.mapper.CouponMapper;
import kwh.cofshop.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupon")
public class CouponController {

    private final CouponService couponService;
    private final CouponMapper couponMapper;

    @Operation(summary = "Get coupon", description = "Get coupon by id")
    @GetMapping("/{couponId}")
    public CouponResponseDto getCouponById(@PathVariable Long couponId) {
        Coupon coupon = couponService.getCouponById(couponId);
        return couponMapper.toResponseDto(coupon);
    }

    @Operation(summary = "Get all coupons", description = "Get all coupons")
    @GetMapping
    public List<CouponResponseDto> getAllCoupons() {
        return couponService.getAllCoupons().stream()
                .map(couponMapper::toResponseDto)
                .toList();
    }

    @Operation(summary = "Create coupon", description = "Create a coupon")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Long> createCoupon(@RequestBody @Valid CouponRequestDto couponRequestDto) {
        Long couponId = couponService.createCoupon(new CreateCouponCommand(
                couponRequestDto.getName(),
                couponRequestDto.getMinOrderPrice(),
                couponRequestDto.getDiscountValue(),
                couponRequestDto.getMaxDiscountAmount(),
                couponRequestDto.getType(),
                couponRequestDto.getCouponCount(),
                couponRequestDto.getValidFrom(),
                couponRequestDto.getValidTo()
        ));
        return ResponseEntity.created(URI.create("/api/coupon")).body(couponId);
    }

    @Operation(summary = "Update coupon state", description = "Change coupon state")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{couponId}/state")
    public ResponseEntity<Void> updateCouponState(
            @PathVariable Long couponId,
            @RequestParam CouponState newState) {
        couponService.updateCouponState(couponId, newState);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Cancel coupon", description = "Cancel coupon issuance")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{couponId}/cancel")
    public ResponseEntity<Void> cancelCoupon(@PathVariable Long couponId) {
        couponService.cancelCoupon(couponId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Expire coupons", description = "Expire coupons manually")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/expire")
    public ResponseEntity<Void> expireCoupons(@RequestParam LocalDate date) {
        couponService.expireCoupons(date);
        return ResponseEntity.noContent().build();
    }
}
