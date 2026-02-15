package kwh.cofshop.coupon.redisstream.service;

import kwh.cofshop.coupon.domain.CouponIssueState;
import kwh.cofshop.coupon.service.MemberCouponIssueService;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LimitedCouponIssueService {

    private final CouponRedisService couponRedisService;
    private final MemberCouponIssueService memberCouponIssueService;

    public CouponIssueState issueCoupon(Long couponId, Long memberId) {
        if (memberCouponIssueService.isAlreadyIssued(memberId, couponId)) {
            // DBÍ∞Ä ?ïÎ≥∏?¥Î?Î°?Redis ?úÏãù??Î≥µÍµ¨??Ï§ëÎ≥µ ?îÏ≤≠??Îπ†Î•¥Í≤?Ï∞®Îã®?úÎã§.
            couponRedisService.saveIssued(couponId, memberId);
            return CouponIssueState.ALREADY_ISSUED;
        }

        CouponIssueState issueState = couponRedisService.issueCoupon(couponId, memberId);
        if (issueState != CouponIssueState.SUCCESS) {
            return issueState;
        }

        try {
            memberCouponIssueService.issueCoupon(memberId, couponId);
            return CouponIssueState.SUCCESS;
        } catch (BusinessException e) {
            couponRedisService.rollbackIssuedCoupon(couponId, memberId);
            if (e.getErrorCode() == BusinessErrorCode.COUPON_ALREADY_EXIST) {
                return CouponIssueState.ALREADY_ISSUED;
            }
            throw e;
        } catch (Exception e) {
            // Redis ?†Ï†ê ?±Í≥µ ??DB ?Ä?•Ïù¥ ?§Ìå®?òÎ©¥ ?òÎüâ/Î∞úÍ∏â?úÏãù???òÎèåÎ¶∞Îã§.
            couponRedisService.rollbackIssuedCoupon(couponId, memberId);
            throw e;
        }
    }
}
