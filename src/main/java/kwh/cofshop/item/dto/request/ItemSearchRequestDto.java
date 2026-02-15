package kwh.cofshop.item.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import kwh.cofshop.global.PageRequestDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemSearchRequestDto extends PageRequestDto {

    @Size(max = 100, message = "상품명 검색어는 100자 이하여야 합니다.")
    private String itemName; // 상품명

    @Positive(message = "카테고리 ID는 양수여야 합니다.")
    private Long categoryId; // 카테고리

}
