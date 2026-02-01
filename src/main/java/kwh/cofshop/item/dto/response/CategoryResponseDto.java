package kwh.cofshop.item.dto.response;

import kwh.cofshop.item.domain.Category;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CategoryResponseDto {
    private Long id;
    private Long parentCategoryId;
    private String name;
    private int depth;

    private List<CategoryResponseDto> children;

    public static CategoryResponseDto from(Category category) {
        CategoryResponseDto dto = new CategoryResponseDto();
        dto.setId(category.getId());
        dto.setParentCategoryId(category.getParent() != null ? category.getParent().getId() : null);
        dto.setName(category.getName());
        dto.setDepth(category.getDepth());
        dto.setChildren(new ArrayList<>());
        return dto;
    }
}
