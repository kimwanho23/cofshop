package kwh.cofshop.item.dto.request;


import kwh.cofshop.global.PageRequestDto;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
public class ItemElasticSearchRequestDto extends PageRequestDto {

    private String keyword;  // 상품명
    private String categoryName;  // 카테고리명
    private Integer minPrice;  // 최소 가격 범위
    private Integer maxPrice;  // 최대 가격 범위 (최소 - 최대 간 Range)
    private Double minRating;  // 최소 평점
    private Double maxRating;  // 최대 평점 Range
    private String origin;  // 원산지


    // 필터링 조건 = (가격, 평점)
    @Builder
    public ItemElasticSearchRequestDto(String keyword, String categoryName, Integer minPrice, Integer maxPrice, Double minRating, Double maxRating, String origin) {
        this.keyword = keyword;
        this.categoryName = categoryName;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.minRating = minRating;
        this.maxRating = maxRating;
        this.origin = origin;
    }
}