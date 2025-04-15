package kwh.cofshop.item.mapper;

import kwh.cofshop.item.domain.Item;
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
    @Mapping(target = "deliveryFee", source = "item.deliveryFee")
    @Mapping(target = "itemState", source = "item.itemState")
    ItemSearchResponseDto toResponseDto(Item item);
}