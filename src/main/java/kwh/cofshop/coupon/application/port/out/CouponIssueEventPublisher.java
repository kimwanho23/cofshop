package kwh.cofshop.coupon.application.port.out;

import kwh.cofshop.coupon.application.port.out.message.CouponIssueEventMessage;

public interface CouponIssueEventPublisher {
    void publish(CouponIssueEventMessage message);
}
