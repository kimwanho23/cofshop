package kwh.cofshop.item.dto.request;

import kwh.cofshop.global.PageRequestDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemSearchRequestDto extends PageRequestDto {

    private String itemName; // 상품명
    private Long categoryId; // 카테고리

}
