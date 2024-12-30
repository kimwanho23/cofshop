package kwh.cofshop.item.mapper;

import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.dto.response.ItemOptionResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;


@Mapper(
        componentModel = SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ItemOptionMapper {

    ItemOptionResponseDto toOptionResponseDto(ItemOption itemOption);
}






