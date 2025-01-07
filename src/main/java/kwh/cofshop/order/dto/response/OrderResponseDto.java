package kwh.cofshop.order.dto.response;

import kwh.cofshop.order.domain.OrderState;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderResponseDto {

    private Long orderId; // 주문 번호
    private OrderState orderState; // 주문 상태
    private OrdererResponseDto ordererResponseDto; // 주문자 상세 정보
    private List<OrderItemResponseDto> orderItemResponseDto; // 주문 상품 정보
}
