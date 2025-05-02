package kwh.cofshop.item.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ItemElasticSearchResponseDto {

    private Long id;
    private String name;
    private int price;
    private String origin;
    private Double averageRating;
    private List<String> categories;

    @Builder
    public ItemElasticSearchResponseDto(Long id, String name, int price, String origin, Double averageRating, List<String> categories) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.origin = origin;
        this.averageRating = averageRating;
        this.categories = categories;
    }
}