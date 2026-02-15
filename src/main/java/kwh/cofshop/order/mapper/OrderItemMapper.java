package kwh.cofshop.order.mapper;

import kwh.cofshop.order.domain.OrderItem;
import kwh.cofshop.order.dto.response.OrderItemResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)

public interface OrderItemMapper {

    @Mapping(source = "item.itemName", target = "itemName")
    @Mapping(source = "orderPrice", target = "price")
    @Mapping(source = "itemOption.additionalPrice", target = "additionalPrice")
    @Mapping(source = "item.origin", target = "origin")
    @Mapping(source = "discountRate", target = "discountRate")
    @Mapping(source = "quantity", target = "quantity")
    OrderItemResponseDto toResponseDto(OrderItem orderItem);

}
