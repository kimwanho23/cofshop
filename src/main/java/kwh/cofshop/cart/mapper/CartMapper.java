package kwh.cofshop.cart.mapper;


import kwh.cofshop.cart.domain.Cart;
import kwh.cofshop.cart.dto.response.CartResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CartMapper {

    CartResponseDto toResponseDto(Cart cart);
}
