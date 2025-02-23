package kwh.cofshop.order.mapper;

import kwh.cofshop.member.domain.Member;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.dto.request.OrderRequestDto;
import kwh.cofshop.order.dto.response.OrderResponseDto;
import kwh.cofshop.order.dto.response.OrdererResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {OrderItemMapper.class}
)
public interface OrderMapper {

    @Mapping(target = "member", source = "member")
    Order toEntity(OrderRequestDto requestDto, Member member);

    @Mapping(target = "ordererResponseDto", source = "order", qualifiedByName = "toOrdererResponseDto")
    @Mapping(target = "orderItemResponseDto", source = "orderItems")
    OrderResponseDto toResponseDto(Order order);


    @Named("toOrdererResponseDto")
    default OrdererResponseDto toOrdererResponseDto(Order order) {
        OrdererResponseDto dto = new OrdererResponseDto();
        dto.setEmail(order.getMember().getEmail());
        dto.setAddress(order.getAddress());
        return dto;
    }
}
