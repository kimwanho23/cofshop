package kwh.cofshop.item.mapper;


import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.dto.response.ItemCreateResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {ItemMapper.class, ItemImgMapper.class, ItemOptionMapper.class}
)
public interface ItemCreateMapper { // 통합 생성 및 매핑

    @Mapping(target = "itemResponseDto", source = "item") // target = dto, source = 엔티티
    @Mapping(target = "imgResponseDto", source = "itemImgs")
    @Mapping(target = "optionResponseDto", source = "itemOptions")
    ItemCreateResponseDto toResponseDto(Item item);
}
