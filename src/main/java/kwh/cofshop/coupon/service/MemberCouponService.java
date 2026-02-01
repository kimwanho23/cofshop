package kwh.cofshop.coupon.service;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.coupon.dto.response.MemberCouponResponseDto;
import kwh.cofshop.coupon.factory.CouponIssuePolicyFactory;
import kwh.cofshop.coupon.mapper.MemberCouponMapper;
import kwh.cofshop.coupon.repository.CouponRepository;
import kwh.cofshop.coupon.repository.MemberCouponRepository;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
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
    private final MemberRepository memberRepository;
    private final MemberCouponRedisService memberCouponRedisService;
    private final MemberCouponMapper memberCouponMapper;
    private final CouponIssuePolicyFactory couponIssuePolicyFactory;


    @Transactional
    // @DistributedLock(keyName = "#memberId + ':' + #couponId")
    public void issueCoupon(Long memberId, Long couponId) {
        log.info("[MemberCouponService] 쿠폰 발급 요청 시작: memberId={}, couponId={}", memberId, couponId);

        // 1. 회원 및 쿠폰 엔티티 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.MEMBER_NOT_FOUND));

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.COUPON_NOT_FOUND));

        LocalDate today = LocalDate.now();
        if (today.isBefore(coupon.getValidFrom()) || today.isAfter(coupon.getValidTo())) {
            throw new BusinessException(BusinessErrorCode.COUPON_NOT_AVAILABLE);
        }

        couponIssuePolicyFactory.getPolicy(coupon).issue(member, coupon);
    }

    // 내 쿠폰 목록
    public List<MemberCouponResponseDto> memberCouponList(Long memberId) {
        List<MemberCoupon> couponList = memberCouponRepository.findByMemberId(memberId);
        return couponList.stream()
                .map(memberCouponMapper::toResponseDto)
                .toList();
    }

    // 유효 쿠폰의 정보 조회
    public MemberCoupon findValidCoupon(Long couponId, Long memberId) {
        return memberCouponRepository.findValidCouponById(couponId, memberId, LocalDate.now())
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.COUPON_NOT_AVAILABLE));
    }

    public MemberCouponResponseDto findValidCouponResponse(Long couponId, Long memberId) {
        MemberCoupon validCoupon = findValidCoupon(couponId, memberId);
        return memberCouponMapper.toResponseDto(validCoupon);
    }

    // 쿠폰 복구
    @Transactional
    public void restoreCoupon(Long memberCouponId) {
        MemberCoupon coupon = memberCouponRepository.findById(memberCouponId).orElseThrow();
        coupon.restoreCouponStatus();
    }

    // 쿠폰 일괄 만료
    @Transactional
    public int expireMemberCoupons(LocalDate today) {
        List<MemberCoupon> expiredCoupons =
                memberCouponRepository.findByCouponExpired(CouponState.AVAILABLE, today);
        expiredCoupons.forEach(MemberCoupon::expireCoupon); // 쿠폰 만료
        return expiredCoupons.size(); // 만료된 쿠폰 수 리턴

    }
}
