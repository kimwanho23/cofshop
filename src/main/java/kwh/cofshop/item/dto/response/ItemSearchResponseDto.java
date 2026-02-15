package kwh.cofshop.item.dto.response;

import kwh.cofshop.item.domain.ItemState;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemSearchResponseDto {

    private String itemName; // 상품명
    private Integer price; // 가격
    private Integer discount; // 할인율 (null 허용)
    private Integer deliveryFee; // 배송비 (null이면 무료)
    private Long categoryId; // 상품 카테고리
    private ItemState itemState; // 아이템 상태
}
