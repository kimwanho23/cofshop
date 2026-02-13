package kwh.cofshop.coupon.controller;

import io.swagger.v3.oas.annotations.Operation;
import kwh.cofshop.argumentResolver.LoginMember;
import kwh.cofshop.coupon.dto.response.MemberCouponResponseDto;
import kwh.cofshop.coupon.service.MemberCouponService;
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
    @Operation(summary = "쿠폰 목록 조회", description = "사용자의 쿠폰 목록을 조회합니다.")
    @PreAuthorize("hasRole('MEMBER')")
    @GetMapping("/me")
    public List<MemberCouponResponseDto> getMemberCouponList(
            @LoginMember Long memberId) {
        return memberCouponService.memberCouponList(memberId);
    }

    // 쿠폰 발급
    @Operation(summary = "쿠폰 발급", description = "사용자에게 쿠폰을 발급합니다.")
    @PreAuthorize("hasRole('MEMBER')")
    @PostMapping("/me/{couponId}")
    public ResponseEntity<Void> createMemberCoupon(
            @LoginMember Long memberId,
            @PathVariable Long couponId) {

        memberCouponService.issueCoupon(memberId, couponId);
        return ResponseEntity
                .created(URI.create("/api/memberCoupon/" + couponId))
                .build();
    }


    // 쿠폰 만료 처리
    @Operation(summary = "쿠폰 만료", description = "회원의 쿠폰 상태를 만료 상태로 변경합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/expire")
    public ResponseEntity<Void> expireMemberCoupons(
            @RequestParam LocalDate date) {
        memberCouponService.expireMemberCoupons(date);
        return ResponseEntity.noContent().build();
    }

}
