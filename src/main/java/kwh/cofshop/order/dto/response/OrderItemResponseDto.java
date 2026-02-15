package kwh.cofshop.order.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemResponseDto {

    private String itemName; // 상품명
    private Integer price; // 가격
    private Integer additionalPrice; // 옵션 추가금
    private Integer discountRate; // 할인율 (null 허용)
    private String origin; // 원산지
    private Integer quantity;  // 수량


}
