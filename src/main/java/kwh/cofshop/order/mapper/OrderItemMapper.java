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
    @Mapping(source = "item.price", target = "price")
    @Mapping(source = "item.discount", target = "discount")
    @Mapping(source = "item.deliveryFee", target = "deliveryFee")
    @Mapping(source = "item.category", target = "categories")
    @Mapping(source = "item.origin", target = "origin")
    @Mapping(source = "quantity", target = "quantity")
    OrderItemResponseDto toResponseDto(OrderItem orderItem);

}
