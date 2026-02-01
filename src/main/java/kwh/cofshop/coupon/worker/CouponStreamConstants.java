package kwh.cofshop.coupon.worker;

public final class CouponStreamConstants {
    public static final String STREAM_KEY = "stream:events";
    public static final String COUPON_GROUP = "consumer-group:coupon";
    public static final String DLQ_STREAM_KEY = "stream:events:dlq";
    public static final String DLQ_GROUP = "consumer-group:coupon-dlq";

    private CouponStreamConstants() {
    }
}
