package kwh.cofshop.coupon.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import kwh.cofshop.config.argumentResolver.LoginMember;
import kwh.cofshop.coupon.dto.response.MemberCouponResponseDto;
import kwh.cofshop.coupon.service.MemberCouponRedisService;
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
    private final MemberCouponRedisService memberCouponRedisService;

    // 내 쿠폰 조회
    @Operation(summary = "쿠폰 목록 조회", description = "사용자의 쿠폰 목록을 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<MemberCouponResponseDto>>> getMemberCouponList(
            @LoginMember CustomUserDetails customUserDetails){
        List<MemberCouponResponseDto> memberCouponListResponseDto = memberCouponService.memberCouponList(customUserDetails.getId());
        return ResponseEntity.ok(ApiResponse.OK(memberCouponListResponseDto));
    }

    // 쿠폰 발급
    @Operation(summary = "쿠폰 발급", description = "사용자에게 쿠폰을 발급합니다.")
    @PostMapping("/me/{couponId}")
    public ResponseEntity<ApiResponse<MemberCouponResponseDto>> createMemberCoupon(
            @LoginMember CustomUserDetails customUserDetails, @PathVariable Long couponId) {
       // memberCouponRedisService.requestCoupon(user.getId(), couponId);
        MemberCouponResponseDto memberCoupon = memberCouponService.createMemberCoupon(customUserDetails.getId(), couponId);
        return ResponseEntity.created(URI.create("/api/memberCoupon" + couponId))
                .body(ApiResponse.Created(memberCoupon));
    }

/*
    // 쿠폰 발급
    @PostMapping("/me/{couponId}")
    public ResponseEntity<ApiResponse<String>> createMemberCoupon(
            @LoginMember CustomUserDetails customUserDetails, @PathVariable Long couponId) throws JsonProcessingException {
        memberCouponRedisService.createMemberCoupon(customUserDetails.getId(), couponId);
        return ResponseEntity.created(URI.create("/api/memberCoupon" + couponId))
                .body(ApiResponse.Created("쿠폰 발급 요청이 처리되었습니다."));
    }
*/

    // 쿠폰 만료 처리
    @Operation(summary = "쿠폰 만료", description = "회원의 쿠폰 상태를 만료 상태로 변경합니다.")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PatchMapping("/expire")
    public ResponseEntity<ApiResponse<Void>> expireMemberCoupons(
            @RequestParam LocalDate date) {
        memberCouponService.expireMemberCoupons(date);
        return ResponseEntity.ok().build();
    }

}
