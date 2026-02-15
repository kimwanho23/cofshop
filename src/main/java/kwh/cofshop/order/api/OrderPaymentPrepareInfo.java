package kwh.cofshop.order.api;

public record OrderPaymentPrepareInfo(
        Long orderId,
        String merchantUid,
        Long finalPrice,
        Long memberId,
        String buyerEmail,
        String buyerName,
        String buyerTel
) {
}
