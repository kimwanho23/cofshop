package kwh.cofshop.coupon.controller;

import io.swagger.v3.oas.annotations.Operation;
import kwh.cofshop.coupon.dto.response.MemberCouponResponseDto;
import kwh.cofshop.coupon.service.MemberCouponService;
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

    @Operation(summary = "Get coupon list", description = "Returns coupons issued to the current member.")
    @PreAuthorize("hasRole('MEMBER')")
    @GetMapping("/me")
    public List<MemberCouponResponseDto> getMemberCouponList(@LoginMember Long memberId) {
        return memberCouponService.memberCouponList(memberId);
    }

    @Operation(summary = "Issue coupon", description = "Issues a coupon to the current member.")
    @PreAuthorize("hasRole('MEMBER')")
    @PostMapping("/me/{couponId}")
    public ResponseEntity<Void> createMemberCoupon(
            @LoginMember Long memberId,
            @PathVariable Long couponId) {
        memberCouponService.issueCoupon(memberId, couponId);
        return ResponseEntity.created(URI.create("/api/memberCoupon/me")).build();
    }

    @Operation(summary = "Expire coupons", description = "Expires member coupons based on the given date.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/expire")
    public ResponseEntity<Void> expireMemberCoupons(@RequestParam LocalDate date) {
        memberCouponService.expireMemberCoupons(date);
        return ResponseEntity.noContent().build();
    }
}
