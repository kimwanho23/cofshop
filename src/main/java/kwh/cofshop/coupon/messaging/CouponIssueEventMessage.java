package kwh.cofshop.coupon.messaging;

public record CouponIssueEventMessage(
        Long outboxEventId,
        Long memberCouponId,
        Long memberId,
        Long couponId
) {
}
