package kwh.cofshop.cart.mapper;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import kwh.cofshop.cart.domain.Cart;
import kwh.cofshop.cart.domain.CartItem;
import kwh.cofshop.cart.dto.response.CartItemResponseDto;
import kwh.cofshop.cart.dto.response.CartResponseDto;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-24T00:40:11+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.9 (Oracle Corporation)"
)
@Component
public class CartMapperImpl implements CartMapper {

    @Override
    public CartResponseDto toResponseDto(Cart cart) {
        if ( cart == null ) {
            return null;
        }

        CartResponseDto cartResponseDto = new CartResponseDto();

        cartResponseDto.setId( cart.getId() );
        cartResponseDto.setCartItems( cartItemListToCartItemResponseDtoList( cart.getCartItems() ) );

        return cartResponseDto;
    }

    protected CartItemResponseDto cartItemToCartItemResponseDto(CartItem cartItem) {
        if ( cartItem == null ) {
            return null;
        }

        CartItemResponseDto cartItemResponseDto = new CartItemResponseDto();

        cartItemResponseDto.setQuantity( cartItem.getQuantity() );

        return cartItemResponseDto;
    }

    protected List<CartItemResponseDto> cartItemListToCartItemResponseDtoList(List<CartItem> list) {
        if ( list == null ) {
            return null;
        }

        List<CartItemResponseDto> list1 = new ArrayList<CartItemResponseDto>( list.size() );
        for ( CartItem cartItem : list ) {
            list1.add( cartItemToCartItemResponseDto( cartItem ) );
        }

        return list1;
    }
}
