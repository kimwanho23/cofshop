package kwh.cofshop.item.mapper;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemImg;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.dto.request.ItemRequestDto;
import kwh.cofshop.item.dto.response.ItemImgResponseDto;
import kwh.cofshop.item.dto.response.ItemOptionResponseDto;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-24T00:40:11+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.9 (Oracle Corporation)"
)
@Component
public class ItemMapperImpl implements ItemMapper {

    @Override
    public Item toEntity(ItemRequestDto dto) {
        if ( dto == null ) {
            return null;
        }

        Item.ItemBuilder item = Item.builder();

        item.itemName( dto.getItemName() );
        item.price( dto.getPrice() );
        item.deliveryFee( dto.getDeliveryFee() );
        item.origin( dto.getOrigin() );
        item.itemLimit( dto.getItemLimit() );

        return item.build();
    }

    @Override
    public ItemResponseDto toResponseDto(Item item) {
        if ( item == null ) {
            return null;
        }

        ItemResponseDto itemResponseDto = new ItemResponseDto();

        itemResponseDto.setImgResponseDto( itemImgListToItemImgResponseDtoList( item.getItemImgs() ) );
        itemResponseDto.setOptionResponseDto( itemOptionListToItemOptionResponseDtoList( item.getItemOptions() ) );
        itemResponseDto.setId( item.getId() );
        itemResponseDto.setItemName( item.getItemName() );
        itemResponseDto.setPrice( item.getPrice() );
        itemResponseDto.setDeliveryFee( item.getDeliveryFee() );
        itemResponseDto.setOrigin( item.getOrigin() );
        itemResponseDto.setItemLimit( item.getItemLimit() );
        itemResponseDto.setItemState( item.getItemState() );

        mapCategoryNames( item, itemResponseDto );

        return itemResponseDto;
    }

    protected ItemImgResponseDto itemImgToItemImgResponseDto(ItemImg itemImg) {
        if ( itemImg == null ) {
            return null;
        }

        ItemImgResponseDto itemImgResponseDto = new ItemImgResponseDto();

        itemImgResponseDto.setId( itemImg.getId() );
        itemImgResponseDto.setImgName( itemImg.getImgName() );
        itemImgResponseDto.setOriImgName( itemImg.getOriImgName() );
        itemImgResponseDto.setImgUrl( itemImg.getImgUrl() );
        itemImgResponseDto.setImgType( itemImg.getImgType() );

        return itemImgResponseDto;
    }

    protected List<ItemImgResponseDto> itemImgListToItemImgResponseDtoList(List<ItemImg> list) {
        if ( list == null ) {
            return null;
        }

        List<ItemImgResponseDto> list1 = new ArrayList<ItemImgResponseDto>( list.size() );
        for ( ItemImg itemImg : list ) {
            list1.add( itemImgToItemImgResponseDto( itemImg ) );
        }

        return list1;
    }

    protected ItemOptionResponseDto itemOptionToItemOptionResponseDto(ItemOption itemOption) {
        if ( itemOption == null ) {
            return null;
        }

        ItemOptionResponseDto itemOptionResponseDto = new ItemOptionResponseDto();

        itemOptionResponseDto.setId( itemOption.getId() );
        itemOptionResponseDto.setDescription( itemOption.getDescription() );
        itemOptionResponseDto.setAdditionalPrice( itemOption.getAdditionalPrice() );
        itemOptionResponseDto.setStock( itemOption.getStock() );

        return itemOptionResponseDto;
    }

    protected List<ItemOptionResponseDto> itemOptionListToItemOptionResponseDtoList(List<ItemOption> list) {
        if ( list == null ) {
            return null;
        }

        List<ItemOptionResponseDto> list1 = new ArrayList<ItemOptionResponseDto>( list.size() );
        for ( ItemOption itemOption : list ) {
            list1.add( itemOptionToItemOptionResponseDto( itemOption ) );
        }

        return list1;
    }
}
