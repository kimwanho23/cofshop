package kwh.cofshop.order.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderCancelRequestDto {

    private Long orderId;
    private String cancelReason; // 취소 사유
}
