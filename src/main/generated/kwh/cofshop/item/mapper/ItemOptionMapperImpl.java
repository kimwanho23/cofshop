package kwh.cofshop.item.mapper;

import javax.annotation.processing.Generated;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.dto.response.ItemOptionResponseDto;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-02-15T13:59:40+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.9 (Oracle Corporation)"
)
@Component
public class ItemOptionMapperImpl implements ItemOptionMapper {

    @Override
    public ItemOptionResponseDto toOptionResponseDto(ItemOption itemOption) {
        if ( itemOption == null ) {
            return null;
        }

        ItemOptionResponseDto itemOptionResponseDto = new ItemOptionResponseDto();

        itemOptionResponseDto.setDescription( itemOption.getDescription() );
        itemOptionResponseDto.setAdditionalPrice( itemOption.getAdditionalPrice() );
        itemOptionResponseDto.setStock( itemOption.getStock() );

        return itemOptionResponseDto;
    }
}
