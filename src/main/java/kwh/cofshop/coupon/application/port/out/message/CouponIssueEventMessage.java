package kwh.cofshop.coupon.application.port.out.message;

public record CouponIssueEventMessage(
        Long outboxEventId,
        Long memberCouponId,
        Long memberId,
        Long couponId
) {
}
