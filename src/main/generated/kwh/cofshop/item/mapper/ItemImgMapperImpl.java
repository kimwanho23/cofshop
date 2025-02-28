package kwh.cofshop.item.mapper;

import javax.annotation.processing.Generated;
import kwh.cofshop.item.domain.ItemImg;
import kwh.cofshop.item.dto.response.ItemImgResponseDto;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-02-15T13:59:40+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.9 (Oracle Corporation)"
)
@Component
public class ItemImgMapperImpl implements ItemImgMapper {

    @Override
    public ItemImgResponseDto toImgResponseDto(ItemImg itemImg) {
        if ( itemImg == null ) {
            return null;
        }

        ItemImgResponseDto itemImgResponseDto = new ItemImgResponseDto();

        itemImgResponseDto.setImgName( itemImg.getImgName() );
        itemImgResponseDto.setOriImgName( itemImg.getOriImgName() );
        itemImgResponseDto.setImgUrl( itemImg.getImgUrl() );
        itemImgResponseDto.setImgType( itemImg.getImgType() );

        return itemImgResponseDto;
    }
}
