package kwh.cofshop.item.mapper;

import kwh.cofshop.item.dto.ItemDocument;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ItemDocumentMapper {
    @Mapping(source = "categories", target = "categoryNames")
    ItemResponseDto toResponseDto(ItemDocument itemDocument);
}
