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
    private String deliveryRequest; // 배송 요청 사항
    private AddressResponseDto address; // 주소

    private Integer deliveryFee;     // 배송비
    private Long totalPrice;         // 총 상품 금액
    private Long discountFromCoupon; // 총 할인 금액
    private Integer usePoint;        // 사용 포인트
    private Long finalPrice;         // 최종 결제 금액 (배송비 포함)

    private List<OrderItemResponseDto> orderItemResponseDto; // 주문 상품 정보
}
