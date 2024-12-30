package kwh.cofshop.order.dto;

import kwh.cofshop.item.domain.ItemState;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRequestDto {

    private String productName; // 상품명
    private String price; // 가격
    private String stock; // 재고 (0일 시 판매 불가)
    private Integer discount; // 할인율 (null 허용)
    private Integer deliveryFee; // 배송비 (null이면 무료)
    private ItemState categories; // 상품 카테고리
    private String origin; // 원산지
    private Integer productLimit; // 수량 제한(한 번에 구입 가능한 개수)
}
