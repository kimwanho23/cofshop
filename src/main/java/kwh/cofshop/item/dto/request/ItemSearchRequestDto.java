package kwh.cofshop.item.dto.request;

import kwh.cofshop.item.domain.Category;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemSearchRequestDto {

    private String itemName; // 상품명
    private Category category; // 카테고리
}
