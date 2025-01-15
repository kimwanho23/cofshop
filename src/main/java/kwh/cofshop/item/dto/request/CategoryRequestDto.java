package kwh.cofshop.item.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequestDto {
    private Long parentCategoryId;
    private String name; // 카테고리 이름
}
