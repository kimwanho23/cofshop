package kwh.cofshop.order.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemRequestDto { // 주문 상품

    private Long item; // 상품 ID
    private Long optionId; // 옵션 ID
    private int orderPrice; // 가격
    private int quantity;  // 수량

}
