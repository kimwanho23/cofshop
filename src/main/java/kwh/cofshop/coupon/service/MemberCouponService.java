package kwh.cofshop.coupon.service;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.coupon.dto.response.MemberCouponResponseDto;
import kwh.cofshop.coupon.mapper.MemberCouponMapper;
import kwh.cofshop.coupon.repository.CouponRepository;
import kwh.cofshop.coupon.repository.MemberCouponRepository;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberCouponService {

    private final MemberCouponRepository memberCouponRepository;
    private final CouponRepository couponRepository;
    private final MemberRepository memberRepository;
    private final MemberCouponMapper memberCouponMapper;

    @Transactional
    public MemberCouponResponseDto createMemberCoupon(Long memberId, Long couponId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.MEMBER_NOT_FOUND));

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.COUPON_NOT_FOUND));

        // 유효기간 확인
        LocalDate today = LocalDate.now();
        if (today.isBefore(coupon.getValidFrom()) || today.isAfter(coupon.getValidTo())) {
            throw new BusinessException(BusinessErrorCode.COUPON_NOT_AVAILABLE);
        }

        MemberCoupon memberCoupon = MemberCoupon.createMemberCoupon(member, coupon);

        try {
            MemberCoupon save = memberCouponRepository.save(memberCoupon);
            return memberCouponMapper.toResponseDto(save);
        } catch (DataIntegrityViolationException e) { // 같은 쿠폰을 여러 장 가질 수 없다.
            throw new BusinessException(BusinessErrorCode.COUPON_ALREADY_EXIST);
        }
    }

    // 내 쿠폰 목록
    public List<MemberCouponResponseDto> memberCouponList(Long memberId){
        List<MemberCoupon> couponList = memberCouponRepository.findByMemberId(memberId);
        return couponList.stream()
                .map(memberCouponMapper::toResponseDto)
                .toList();
    }

    // 유효 쿠폰의 정보 조회
    public MemberCoupon findValidCoupon(Long couponId, Long memberId){
        return memberCouponRepository.findValidCouponById(couponId, memberId, LocalDate.now())
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.COUPON_NOT_AVAILABLE));
    }

    @Transactional
    public void expireMemberCoupons(LocalDate today) {
        List<MemberCoupon> expiredCoupons =
                memberCouponRepository.findByCouponExpired(CouponState.AVAILABLE, today);

        expiredCoupons.forEach(MemberCoupon::expireCoupon); // 쿠폰 만료
    }
}
