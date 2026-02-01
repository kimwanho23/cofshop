package kwh.cofshop.cart.mapper;

import javax.annotation.processing.Generated;
import kwh.cofshop.cart.domain.Cart;
import kwh.cofshop.cart.domain.CartItem;
import kwh.cofshop.cart.dto.request.CartItemRequestDto;
import kwh.cofshop.cart.dto.response.CartItemResponseDto;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-24T00:40:11+0900",
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

        cartItemResponseDto.setCartId( cartItemCartId( cartItem ) );
        cartItemResponseDto.setItemId( cartItemItemId( cartItem ) );
        cartItemResponseDto.setOptionId( cartItemItemOptionId( cartItem ) );
        cartItemResponseDto.setQuantity( cartItem.getQuantity() );

        return cartItemResponseDto;
    }

    private Long cartItemCartId(CartItem cartItem) {
        if ( cartItem == null ) {
            return null;
        }
        Cart cart = cartItem.getCart();
        if ( cart == null ) {
            return null;
        }
        Long id = cart.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Long cartItemItemId(CartItem cartItem) {
        if ( cartItem == null ) {
            return null;
        }
        Item item = cartItem.getItem();
        if ( item == null ) {
            return null;
        }
        Long id = item.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Long cartItemItemOptionId(CartItem cartItem) {
        if ( cartItem == null ) {
            return null;
        }
        ItemOption itemOption = cartItem.getItemOption();
        if ( itemOption == null ) {
            return null;
        }
        Long id = itemOption.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
