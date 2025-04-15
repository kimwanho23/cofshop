package kwh.cofshop.item.dto.request;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemOptionRequestDto {

    private Long id;
    private String description; // 옵션 내용
    private Integer additionalPrice; // 추가금 (기본금에 더해서)
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
