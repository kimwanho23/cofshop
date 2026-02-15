package kwh.cofshop.item.mapper;

import kwh.cofshop.item.domain.Category;
import kwh.cofshop.item.dto.request.CategoryRequestDto;
import kwh.cofshop.item.dto.response.CategoryResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {ItemMapper.class, ItemImgMapper.class, ItemOptionMapper.class}
)
public interface CategoryMapper {

    @Mapping(source = "parent.id", target = "parentCategoryId")
    @Mapping(target = "children", ignore = true)
    CategoryResponseDto toResponseDto(Category category);

    @Mapping(target = "parent", ignore = true)
    Category toEntity(CategoryRequestDto requestDto);
}
