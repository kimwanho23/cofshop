package kwh.cofshop.order.mapper;

import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.Address;
import kwh.cofshop.order.dto.response.AddressResponseDto;
import kwh.cofshop.order.dto.response.OrderResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {OrderItemMapper.class}
)
public interface OrderMapper {

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "orderItems", source = "orderItems")
    OrderResponseDto toResponseDto(Order order);

    AddressResponseDto toAddressResponseDto(Address address);
}
