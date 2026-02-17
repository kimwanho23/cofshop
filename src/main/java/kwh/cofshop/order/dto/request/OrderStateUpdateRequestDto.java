package kwh.cofshop.order.dto.request;

import jakarta.validation.constraints.NotNull;
import kwh.cofshop.order.domain.OrderState;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderStateUpdateRequestDto {

    @NotNull(message = "변경할 주문 상태는 필수입니다.")
    private OrderState orderState;
}
