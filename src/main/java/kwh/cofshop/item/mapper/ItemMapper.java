package kwh.cofshop.item.mapper;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.dto.request.ItemRequestDto;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import org.mapstruct.*;


import java.util.List;
import java.util.stream.Collectors;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;


@Mapper(
        componentModel = SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ItemMapper {
    // DTO → 엔티티 매핑

    Item toEntity(ItemRequestDto dto);

    @Mapping(target = "imgResponseDto", source = "itemImgs")
    @Mapping(target = "optionResponseDto", source = "itemOptions")
    ItemResponseDto toResponseDto(Item item);

    @AfterMapping
    default void mapCategoryNames(Item item, @MappingTarget ItemResponseDto dto) {
        if (item.getItemCategories() != null) {
            List<String> categoryNames = item.getItemCategories().stream()
                    .map(itemCategory -> itemCategory.getCategory().getName())
                    .collect(Collectors.toList());
            dto.setCategoryNames(categoryNames);
        }
    }
}






