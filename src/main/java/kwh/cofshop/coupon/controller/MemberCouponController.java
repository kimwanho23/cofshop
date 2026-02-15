package kwh.cofshop.coupon.controller;

import io.swagger.v3.oas.annotations.Operation;
import kwh.cofshop.coupon.service.MemberCouponService;
import kwh.cofshop.coupon.dto.response.MemberCouponResponseDto;
import kwh.cofshop.coupon.mapper.MemberCouponMapper;
import kwh.cofshop.global.annotation.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/memberCoupon")
public class MemberCouponController {

    private final MemberCouponService memberCouponService;
    private final MemberCouponMapper memberCouponMapper;

    @Operation(summary = "쿠폰 목록 조회", description = "?�용?�의 쿠폰 목록??조회?�니??")
    @PreAuthorize("hasRole('MEMBER')")
    @GetMapping("/me")
    public List<MemberCouponResponseDto> getMemberCouponList(@LoginMember Long memberId) {
        return memberCouponService.memberCouponList(memberId).stream()
                .map(memberCouponMapper::toResponseDto)
                .toList();
    }

    @Operation(summary = "쿠폰 발급", description = "?�용?�에�?쿠폰??발급?�니??")
    @PreAuthorize("hasRole('MEMBER')")
    @PostMapping("/me/{couponId}")
    public ResponseEntity<Void> createMemberCoupon(
            @LoginMember Long memberId,
            @PathVariable Long couponId) {
        memberCouponService.issueCoupon(memberId, couponId);
        return ResponseEntity.created(URI.create("/api/memberCoupon/me")).build();
    }

    @Operation(summary = "쿠폰 만료", description = "?�원??쿠폰 ?�태�?만료 ?�태�?변경합?�다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/expire")
    public ResponseEntity<Void> expireMemberCoupons(@RequestParam LocalDate date) {
        memberCouponService.expireMemberCoupons(date);
        return ResponseEntity.noContent().build();
    }
}
