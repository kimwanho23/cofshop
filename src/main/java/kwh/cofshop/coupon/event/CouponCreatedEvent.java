package kwh.cofshop.coupon.event;

public record CouponCreatedEvent(Long couponId, Integer couponCount) {
    public boolean isLimited() {
        return couponCount != null;
    }
}