package kwh.cofshop.item.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequestDto {
    private Long parentCategoryId;

    @NotBlank(message = "카테고리 이름은 필수입니다.")
    private String name; // 카테고리 이름

    @Min(value = 0, message = "깊이는 0 이상이어야 합니다.")
    private int depth; // 계층 깊이
}
