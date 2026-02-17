package kwh.cofshop.order.dto.response;

import lombok.Getter;

@Getter
public class OrderCancelResponseDto {

    private final Long orderId;
    private final String cancelReason;

    private OrderCancelResponseDto(Long orderId, String cancelReason) {
        this.orderId = orderId;
        this.cancelReason = cancelReason;
    }

    public static OrderCancelResponseDto of(Long orderId, String cancelReason) {
        return new OrderCancelResponseDto(orderId, cancelReason);
    }
}
