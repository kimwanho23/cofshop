package kwh.cofshop.order.dto.response;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderCancelResponseDto {

    private Long orderId;
    private String cancelReason;
}
