package kwh.cofshop.coupon.service;

import kwh.cofshop.coupon.service.outbox.CouponIssueOutboxService;
import kwh.cofshop.coupon.repository.CouponRepository;
import kwh.cofshop.coupon.repository.MemberCouponRepository;
import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.member.api.MemberReadPort;
import kwh.cofshop.member.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MemberCouponIssueService {

    private final MemberCouponRepository memberCouponRepository;
    private final CouponRepository couponRepository;
    private final MemberReadPort memberReadPort;
    private final CouponIssueOutboxService couponIssueOutboxService;

    @Transactional(readOnly = true)
    public boolean isAlreadyIssued(Long memberId, Long couponId) {
        return memberCouponRepository.existsByMember_IdAndCoupon_Id(memberId, couponId);
    }


    @Transactional
    public Long issueCoupon(Long memberId, Long couponId) {
        Member member = memberReadPort.getById(memberId);
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.COUPON_NOT_FOUND));

        validateCouponIssuableAtIssuance(memberId, couponId, coupon);
        decreaseLimitedCouponStock(coupon);

        try {
            MemberCoupon save = memberCouponRepository.saveAndFlush(MemberCoupon.createMemberCoupon(member, coupon));
            couponIssueOutboxService.enqueueCouponIssued(save.getId(), memberId, couponId);
            return save.getId();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(BusinessErrorCode.COUPON_ALREADY_EXIST);
        }
    }

    private void decreaseLimitedCouponStock(Coupon coupon) {
        if (!coupon.isLimitedQuantity()) {
            return;
        }

        int updatedRows = couponRepository.decreaseCouponCount(coupon.getId());
        if (updatedRows == 0) {
            throw new BusinessException(BusinessErrorCode.COUPON_RUN_OUT);
        }
    }

    private void validateCouponIssuableAtIssuance(Long memberId, Long couponId, Coupon coupon) {
        LocalDate today = LocalDate.now();

        if (coupon.getState() != CouponState.AVAILABLE) {
            throw new BusinessException(BusinessErrorCode.COUPON_NOT_AVAILABLE);
        }

        if (today.isBefore(coupon.getValidFrom()) || today.isAfter(coupon.getValidTo())) {
            throw new BusinessException(BusinessErrorCode.COUPON_NOT_AVAILABLE);
        }

        if (memberCouponRepository.existsByMember_IdAndCoupon_Id(memberId, couponId)) {
            throw new BusinessException(BusinessErrorCode.COUPON_ALREADY_EXIST);
        }
    }

}
