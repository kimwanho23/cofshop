package kwh.cofshop.item.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemOptionRequestDto {

    private Long id;

    @NotBlank(message = "옵션 설명은 필수입니다.")
    private String description; // 옵션 내용

    @PositiveOrZero(message = "추가금은 0 이상이어야 합니다.")
    private Integer additionalPrice; // 추가금(기본금에 대해)

    @PositiveOrZero(message = "재고는 0 이상이어야 합니다.")
    private Integer stock; // 옵션별 재고

    public ItemOptionRequestDto() {
    }

    @Builder
    public ItemOptionRequestDto(Long id, String description, Integer additionalPrice, Integer stock) {
        this.id = id;
        this.description = description;
        this.additionalPrice = additionalPrice;
        this.stock = stock;
    }
}
