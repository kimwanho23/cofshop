package kwh.cofshop.item.mapper;

import kwh.cofshop.item.domain.Category;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemCategory;
import kwh.cofshop.item.domain.ItemState;
import kwh.cofshop.item.dto.response.ItemSearchResponseDto;
import kwh.cofshop.member.domain.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class ItemSearchMapperTest {

    private final ItemSearchMapper mapper = Mappers.getMapper(ItemSearchMapper.class);

    @Test
    @DisplayName("검색 매핑: 카테고리가 있으면 categoryId를 채운다")
    void toResponseDto_withCategory() {
        Item item = createItem();
        Category category = Category.builder().name("원두").build();
        ReflectionTestUtils.setField(category, "id", 10L);
        item.getItemCategories().add(ItemCategory.builder().item(item).category(category).build());

        ItemSearchResponseDto dto = mapper.toResponseDto(item);

        assertThat(dto.getCategoryId()).isEqualTo(10L);
        assertThat(dto.getItemName()).isEqualTo("커피");
    }

    @Test
    @DisplayName("검색 매핑: 카테고리가 없으면 categoryId는 null")
    void toResponseDto_withoutCategory() {
        ItemSearchResponseDto dto = mapper.toResponseDto(createItem());

        assertThat(dto.getCategoryId()).isNull();
    }

    private Item createItem() {
        Item item = Item.builder()
                .itemName("커피")
                .price(1500)
                .discount(10)
                .deliveryFee(0)
                .itemCategories(new ArrayList<>())
                .origin("브라질")
                .itemLimit(10)
                .seller(Member.builder()
                        .id(1L)
                        .email("seller@example.com")
                        .memberName("seller")
                        .memberPwd("pw")
                        .tel("01012341234")
                        .build())
                .build();
        ReflectionTestUtils.setField(item, "itemState", ItemState.SELL);
        return item;
    }
}
