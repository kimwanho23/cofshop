package kwh.cofshop.coupon.messaging;

public interface CouponIssueEventPublisher {
    void publish(CouponIssueEventMessage message);
}
