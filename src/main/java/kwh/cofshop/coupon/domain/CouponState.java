package kwh.cofshop.coupon.domain;

public enum CouponState {
    AVAILABLE,   // 사용 가능
    USED,        // 사용 완료
    EXPIRED,     // 유효기간 초과
    CANCELLED    // 취소/삭제됨
}
