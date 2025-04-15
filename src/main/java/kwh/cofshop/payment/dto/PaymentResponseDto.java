package kwh.cofshop.payment.dto;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class PaymentResponseDto {
    private String merchantUid;    // 주문번호 (Order - OrderId)
    private String productName;    // 상품명(OrderItem - itemName)
    private int amount;            // 결제 총액 (Order - totalPrice)
    private String buyerEmail; // 구매자 이메일 (Member - email)
    private String buyerName; // 구매자 이름(Member - memberName)
    private String buyerTel; // 구매자 전화번호 (Member - tel)
}

