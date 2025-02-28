package kwh.cofshop.item.mapper;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.dto.request.ItemRequestDto;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;


import java.util.Collections;
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
    @Mapping(target = "categoryNames", expression = "java(getCategoryNames(item))")
    ItemResponseDto toResponseDto(Item item);

    default List<String> getCategoryNames(Item item) {
        if (item.getItemCategories() == null) {
            return Collections.emptyList();
        }
        return item.getItemCategories().stream()
                .map(itemCategory -> itemCategory.getCategory().getName())
                .collect(Collectors.toList());
    }
}






