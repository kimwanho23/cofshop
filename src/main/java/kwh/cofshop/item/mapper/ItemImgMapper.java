package kwh.cofshop.item.mapper;

import kwh.cofshop.item.domain.ItemImg;
import kwh.cofshop.item.dto.response.ItemImgResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ItemImgMapper {

    ItemImgResponseDto toImgResponseDto(ItemImg itemImg);

}
