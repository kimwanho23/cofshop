package kwh.cofshop.item.dto;


import jakarta.persistence.Id;
import kwh.cofshop.item.domain.Item;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.*;

import java.util.List;

@Getter
@Document(indexName = "items", createIndex = true)
@Setting(settingPath = "elasticsearch/Item-setting.json")
@Mapping(mappingPath = "elasticsearch/Item-mapping.json")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemDocument {

    @Id
    @Field(type = FieldType.Long)
    private Long id;

    @Field(type = FieldType.Text)
    private String itemName; // 상품명

    @Field(type = FieldType.Integer)
    private int price; // 가격

    @Field(type = FieldType.Keyword)
    private String origin; // 원산지

    @Field(type = FieldType.Double)
    private Double averageRating;  // 평균 평점

    @Field(type = FieldType.Keyword)
    private List<String> categories; // 카테고리


    @Builder
    public ItemDocument(Long id, String itemName, int price, String origin, Double averageRating, List<String> categories) {
        this.id = id;
        this.itemName = itemName;
        this.price = price;
        this.origin = origin;
        this.averageRating = averageRating;
        this.categories = categories;
    }

    public static ItemDocument of(Item item) {
        return ItemDocument.builder()
                .id(item.getId())
                .itemName(item.getItemName())
                .price(item.getPrice())
                .origin(item.getOrigin())
                .averageRating(item.getAverageRating())
                .categories(
                        item.getItemCategories().stream()
                                .map(ic -> ic.getCategory().getName())
                                .toList()
                )
                .build();
    }
}
