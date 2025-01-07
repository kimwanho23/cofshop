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
    @Mapping(target = "item", source = "item")
    @Mapping(target = "itemOption", source = "itemOption")
    OrderItemResponseDto toResponseDto(OrderItem orderItem);

}
