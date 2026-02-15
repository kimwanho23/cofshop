package kwh.cofshop.cart.mapper;


import kwh.cofshop.cart.domain.CartItem;
import kwh.cofshop.cart.dto.request.CartItemRequestDto;
import kwh.cofshop.cart.dto.response.CartItemResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CartItemMapper {

    CartItem toEntity(CartItemRequestDto cartItemRequestDto);


    @Mapping(target = "cartId", source = "cart.id")
    @Mapping(target = "itemId", source = "item.id")
    @Mapping(target = "optionId", source = "itemOption.id")
    CartItemResponseDto toResponseDto(CartItem cartItem);


}
