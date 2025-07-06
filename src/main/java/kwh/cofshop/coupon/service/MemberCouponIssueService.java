package kwh.cofshop.coupon.service;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.coupon.factory.CouponIssuePolicyFactory;
import kwh.cofshop.coupon.mapper.MemberCouponMapper;
import kwh.cofshop.coupon.repository.CouponRepository;
import kwh.cofshop.coupon.repository.MemberCouponRepository;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberCouponIssueService {

    private final MemberCouponRepository memberCouponRepository;
    private final CouponRepository couponRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Long issueLimitedCoupon(Long memberId, Long couponId){
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.MEMBER_NOT_FOUND));
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.COUPON_NOT_FOUND));

        MemberCoupon memberCoupon = MemberCoupon.createMemberCoupon(member, coupon);
        MemberCoupon save = memberCouponRepository.save(memberCoupon);
        couponRepository.decreaseCouponCount(coupon.getId());
        return save.getId();
    }

    @Transactional
    public Long issueUnLimitedCoupon(Long memberId, Long couponId){
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.MEMBER_NOT_FOUND));
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.COUPON_NOT_FOUND));

        MemberCoupon memberCoupon = MemberCoupon.createMemberCoupon(member, coupon);
        MemberCoupon save = memberCouponRepository.save(memberCoupon);
        return save.getId();
    }

}
