package kwh.cofshop.coupon.infrastructure.legacy.redisstream.service;

import kwh.cofshop.coupon.domain.CouponIssueState;
import kwh.cofshop.coupon.application.service.MemberCouponIssueService;
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
            // DB가 정본이므로 Redis 표식을 복구해 중복 요청을 빠르게 차단한다.
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
            // Redis 선점 성공 후 DB 저장이 실패하면 수량/발급표식을 되돌린다.
            couponRedisService.rollbackIssuedCoupon(couponId, memberId);
            throw e;
        }
    }
}
