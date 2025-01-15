package kwh.cofshop.item.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CategoryResponseDto {

    private Long id;
    private Long parentCategoryId;
    private String name;
    private int depth;
    private List<CategoryResponseDto> children;
}
