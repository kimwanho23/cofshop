package kwh.cofshop.cart.mapper;

import javax.annotation.processing.Generated;
import kwh.cofshop.cart.domain.CartItem;
import kwh.cofshop.cart.dto.request.CartItemRequestDto;
import kwh.cofshop.cart.dto.response.CartItemResponseDto;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.dto.response.ItemOptionResponseDto;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-02-15T13:59:40+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.9 (Oracle Corporation)"
)
@Component
public class CartItemMapperImpl implements CartItemMapper {

    @Override
    public CartItem toEntity(CartItemRequestDto cartItemRequestDto) {
        if ( cartItemRequestDto == null ) {
            return null;
        }

        CartItem.CartItemBuilder cartItem = CartItem.builder();

        cartItem.quantity( cartItemRequestDto.getQuantity() );

        return cartItem.build();
    }

    @Override
    public CartItemResponseDto toResponseDto(CartItem cartItem) {
        if ( cartItem == null ) {
            return null;
        }

        CartItemResponseDto cartItemResponseDto = new CartItemResponseDto();

        cartItemResponseDto.setItem( itemToItemResponseDto( cartItem.getItem() ) );
        cartItemResponseDto.setItemOption( itemOptionToItemOptionResponseDto( cartItem.getItemOption() ) );
        cartItemResponseDto.setQuantity( cartItem.getQuantity() );

        return cartItemResponseDto;
    }

    protected ItemResponseDto itemToItemResponseDto(Item item) {
        if ( item == null ) {
            return null;
        }

        ItemResponseDto itemResponseDto = new ItemResponseDto();

        itemResponseDto.setItemName( item.getItemName() );
        itemResponseDto.setPrice( item.getPrice() );
        itemResponseDto.setDiscount( item.getDiscount() );
        itemResponseDto.setDeliveryFee( item.getDeliveryFee() );
        itemResponseDto.setOrigin( item.getOrigin() );
        itemResponseDto.setItemLimit( item.getItemLimit() );
        itemResponseDto.setItemState( item.getItemState() );

        return itemResponseDto;
    }

    protected ItemOptionResponseDto itemOptionToItemOptionResponseDto(ItemOption itemOption) {
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
