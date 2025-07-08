package kwh.cofshop.coupon.domain;

public enum CouponIssueState {
    SUCCESS, // 발급 성공
    ALREADY_ISSUED, // 이미 발급됨
    OUT_OF_STOCK // 재고 없음
}
