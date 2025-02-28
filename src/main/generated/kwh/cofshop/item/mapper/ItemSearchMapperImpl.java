package kwh.cofshop.item.mapper;

import javax.annotation.processing.Generated;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.dto.response.ItemSearchResponseDto;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-02-15T13:59:40+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.9 (Oracle Corporation)"
)
@Component
public class ItemSearchMapperImpl implements ItemSearchMapper {

    @Override
    public ItemSearchResponseDto toResponseDto(Item item) {
        if ( item == null ) {
            return null;
        }

        ItemSearchResponseDto itemSearchResponseDto = new ItemSearchResponseDto();

        itemSearchResponseDto.setItemName( item.getItemName() );
        itemSearchResponseDto.setPrice( item.getPrice() );
        itemSearchResponseDto.setDiscount( item.getDiscount() );
        itemSearchResponseDto.setDeliveryFee( item.getDeliveryFee() );
        itemSearchResponseDto.setCategories( item.getCategory() );
        itemSearchResponseDto.setItemState( item.getItemState() );

        return itemSearchResponseDto;
    }
}
