package kwh.cofshop.order.dto.response;

import kwh.cofshop.item.domain.Category;
import kwh.cofshop.item.domain.ItemState;
import kwh.cofshop.item.dto.response.ItemOptionResponseDto;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import kwh.cofshop.order.domain.OrderState;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemResponseDto {

    private String itemName; // 상품명
    private Integer price; // 가격
    private Integer discount; // 할인율 (null 허용)
    private Integer deliveryFee; // 배송비 (null이면 무료)
    private Category categories; // 상품 카테고리
    private String origin; // 원산지
    private Integer quantity;  // 수량


}
