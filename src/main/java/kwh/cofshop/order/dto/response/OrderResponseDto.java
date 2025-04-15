package kwh.cofshop.order.dto.response;

import kwh.cofshop.order.domain.Address;
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
    private Address address; // 주소

    private int deliveryFee;     // 배송비
    private int totalPrice;      // 총 상품 금액
    private int discountFromCoupon;  // 총 할인 금액 (쿠폰 + 포인트 + 할인 금액 적용)
    private int usePoint;        // 사용 포인트
    private int finalPrice;      // 최종 결제 금액 (배송비 포함)

    private List<OrderItemResponseDto> orderItemResponseDto; // 주문 상품 정보
}
