package kwh.cofshop.item.mapper;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import kwh.cofshop.item.dto.ItemDocument;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-24T00:40:11+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.9 (Oracle Corporation)"
)
@Component
public class ItemDocumentMapperImpl implements ItemDocumentMapper {

    @Override
    public ItemResponseDto toResponseDto(ItemDocument itemDocument) {
        if ( itemDocument == null ) {
            return null;
        }

        ItemResponseDto itemResponseDto = new ItemResponseDto();

        List<String> list = itemDocument.getCategories();
        if ( list != null ) {
            itemResponseDto.setCategoryNames( new ArrayList<String>( list ) );
        }
        itemResponseDto.setId( itemDocument.getId() );
        itemResponseDto.setItemName( itemDocument.getItemName() );
        itemResponseDto.setPrice( itemDocument.getPrice() );
        itemResponseDto.setOrigin( itemDocument.getOrigin() );

        return itemResponseDto;
    }
}
