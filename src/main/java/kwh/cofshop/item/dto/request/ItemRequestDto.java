package kwh.cofshop.item.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemRequestDto {

    private String itemName; // 상품명
    private Integer price; // 가격
    private Integer discount; // 할인율 (null 허용)
    private Integer deliveryFee; // 배송비 (null이면 무료)
    private String categories; // 상품 카테고리
    private String origin; // 원산지
    private Integer itemLimit; // 수량 제한(한 번에 구입 가능한 개수)
}
