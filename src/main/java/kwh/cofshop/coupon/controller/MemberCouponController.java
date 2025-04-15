package kwh.cofshop.coupon.controller;

import kwh.cofshop.config.argumentResolver.LoginMember;
import kwh.cofshop.coupon.dto.response.MemberCouponResponseDto;
import kwh.cofshop.coupon.service.MemberCouponService;
import kwh.cofshop.global.response.ApiResponse;
import kwh.cofshop.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/memberCoupon")
public class MemberCouponController {

    private final MemberCouponService memberCouponService;

    // 내 쿠폰 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<MemberCouponResponseDto>>> getMemberCouponList(
            @LoginMember CustomUserDetails customUserDetails){
        List<MemberCouponResponseDto> memberCouponListResponseDto = memberCouponService.memberCouponList(customUserDetails.getId());
        return ResponseEntity.ok(ApiResponse.OK(memberCouponListResponseDto));
    }

    // 쿠폰 발급
    @PostMapping("/me/{couponId}")
    public ResponseEntity<ApiResponse<MemberCouponResponseDto>> createMemberCoupon(
            @LoginMember CustomUserDetails customUserDetails, @PathVariable Long couponId) {
        MemberCouponResponseDto memberCoupon = memberCouponService.createMemberCoupon(customUserDetails.getId(), couponId);
        return ResponseEntity.created(URI.create("/api/memberCoupon" + couponId))
                .body(ApiResponse.Created(memberCoupon));
    }

    // 쿠폰 만료 처리
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PatchMapping("/expire")
    public ResponseEntity<ApiResponse<Void>> expireMemberCoupons(
            @RequestParam LocalDate date) {
        memberCouponService.expireMemberCoupons(date);
        return ResponseEntity.ok().build();
    }

}
