package kwh.cofshop.order.dto.response;

import kwh.cofshop.order.domain.OrderState;
import kwh.cofshop.order.domain.OrderRefundRequestStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
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
    private OrderRefundRequestStatus refundRequestStatus; // 환불 요청 상태
    private String refundRequestReason;      // 환불 요청 사유
    private LocalDateTime refundRequestedAt; // 환불 요청 시각
    private LocalDateTime refundProcessedAt; // 환불 요청 처리 시각
    private String refundProcessedReason; // 환불 요청 처리 사유

    private List<OrderItemResponseDto> orderItems; // 주문 상품 정보
}
