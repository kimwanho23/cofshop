package kwh.cofshop.item.mapper;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemCategory;
import kwh.cofshop.item.dto.response.ItemSearchResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ItemSearchMapper {

    @Mapping(target = "itemName", source = "item.itemName")
    @Mapping(target = "price", source = "item.price")
    @Mapping(target = "discount", source = "item.discount")
    @Mapping(target = "deliveryFee", source = "item.deliveryFee")
    @Mapping(target = "itemState", source = "item.itemState")
    @Mapping(target = "categoryId", expression = "java(toPrimaryCategoryId(item))")
    ItemSearchResponseDto toResponseDto(Item item);

    default Long toPrimaryCategoryId(Item item) {
        if (item.getItemCategories() == null || item.getItemCategories().isEmpty()) {
            return null;
        }
        ItemCategory itemCategory = item.getItemCategories().get(0);
        if (itemCategory == null || itemCategory.getCategory() == null) {
            return null;
        }
        return itemCategory.getCategory().getId();
    }
}
