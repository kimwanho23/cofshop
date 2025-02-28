package kwh.cofshop.item.mapper;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import kwh.cofshop.item.domain.Category;
import kwh.cofshop.item.dto.request.CategoryRequestDto;
import kwh.cofshop.item.dto.response.CategoryResponseDto;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-02-15T13:59:40+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.9 (Oracle Corporation)"
)
@Component
public class CategoryMapperImpl implements CategoryMapper {

    @Override
    public CategoryResponseDto toResponseDto(Category category) {
        if ( category == null ) {
            return null;
        }

        CategoryResponseDto categoryResponseDto = new CategoryResponseDto();

        categoryResponseDto.setParentCategoryId( categoryParentId( category ) );
        categoryResponseDto.setId( category.getId() );
        categoryResponseDto.setName( category.getName() );
        categoryResponseDto.setDepth( category.getDepth() );
        categoryResponseDto.setChildren( categoryListToCategoryResponseDtoList( category.getChildren() ) );

        return categoryResponseDto;
    }

    @Override
    public Category toEntity(CategoryRequestDto requestDto) {
        if ( requestDto == null ) {
            return null;
        }

        Category.CategoryBuilder category = Category.builder();

        category.name( requestDto.getName() );

        return category.build();
    }

    private Long categoryParentId(Category category) {
        if ( category == null ) {
            return null;
        }
        Category parent = category.getParent();
        if ( parent == null ) {
            return null;
        }
        Long id = parent.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected List<CategoryResponseDto> categoryListToCategoryResponseDtoList(List<Category> list) {
        if ( list == null ) {
            return null;
        }

        List<CategoryResponseDto> list1 = new ArrayList<CategoryResponseDto>( list.size() );
        for ( Category category : list ) {
            list1.add( toResponseDto( category ) );
        }

        return list1;
    }
}
