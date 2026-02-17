package kwh.cofshop.item.mapper;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.dto.request.ItemRequestDto;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import org.mapstruct.*;

import java.util.List;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;


@Mapper(
        componentModel = SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ItemMapper {
    // DTO → 엔티티 매핑

    Item toEntity(ItemRequestDto dto);

    @Mapping(target = "itemImages", source = "itemImgs")
    @Mapping(target = "itemOptions", source = "itemOptions")
    @Mapping(target = "email", source = "seller.email")
    @Mapping(target = "categoryNames", expression = "java(toCategoryNames(item))")
    @Mapping(target = "categoryId", expression = "java(toPrimaryCategoryId(item))")
    ItemResponseDto toResponseDto(Item item);

    default List<String> toCategoryNames(Item item) {
        if (item.getItemCategories() == null || item.getItemCategories().isEmpty()) {
            return List.of();
        }
        return item.getItemCategories().stream()
                .map(itemCategory -> itemCategory.getCategory().getName())
                .toList();
    }

    default Long toPrimaryCategoryId(Item item) {
        if (item.getItemCategories() == null || item.getItemCategories().isEmpty()) {
            return null;
        }
        if (item.getItemCategories().get(0).getCategory() == null) {
            return null;
        }
        return item.getItemCategories().get(0).getCategory().getId();
    }
}






