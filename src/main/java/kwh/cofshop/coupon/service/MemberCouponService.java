package kwh.cofshop.coupon.service;

import kwh.cofshop.coupon.service.factory.CouponIssuePolicyFactory;
import kwh.cofshop.coupon.repository.CouponRepository;
import kwh.cofshop.coupon.repository.MemberCouponRepository;
import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.coupon.dto.response.MemberCouponResponseDto;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.member.api.MemberReadPort;
import kwh.cofshop.member.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberCouponService {

    private final MemberCouponRepository memberCouponRepository;
    private final CouponRepository couponRepository;
    private final MemberReadPort memberReadPort;
    private final CouponIssuePolicyFactory couponIssuePolicyFactory;

    @Transactional
    public void issueCoupon(Long memberId, Long couponId) {
        log.info("[MemberCouponService] coupon issue requested: memberId={}, couponId={}", memberId, couponId);

        Member member = memberReadPort.getById(memberId);
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.COUPON_NOT_FOUND));

        if (coupon.getState() != CouponState.AVAILABLE) {
            throw new BusinessException(BusinessErrorCode.COUPON_NOT_AVAILABLE);
        }

        LocalDate today = LocalDate.now();
        if (today.isBefore(coupon.getValidFrom()) || today.isAfter(coupon.getValidTo())) {
            throw new BusinessException(BusinessErrorCode.COUPON_NOT_AVAILABLE);
        }

        couponIssuePolicyFactory.getPolicy(coupon).issue(member, coupon);
    }

    public List<MemberCouponResponseDto> memberCouponList(Long memberId) {
        return memberCouponRepository.findResponseByMemberId(memberId);
    }

    public MemberCoupon findValidCoupon(Long memberCouponId, Long memberId) {
        return memberCouponRepository.findValidCouponByMemberCouponId(memberCouponId, memberId, LocalDate.now())
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.COUPON_NOT_AVAILABLE));
    }

    @Transactional
    public void restoreCoupon(Long memberCouponId) {
        MemberCoupon coupon = memberCouponRepository.findById(memberCouponId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.COUPON_NOT_FOUND));
        coupon.restoreCouponStatus();
    }

    @Transactional
    public int expireMemberCoupons(LocalDate today) {
        List<MemberCoupon> expiredCoupons = memberCouponRepository.findByCouponExpired(CouponState.AVAILABLE, today);
        expiredCoupons.forEach(MemberCoupon::expireCoupon);
        return expiredCoupons.size();
    }
}
