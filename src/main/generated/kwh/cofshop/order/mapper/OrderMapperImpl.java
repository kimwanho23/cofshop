package kwh.cofshop.order.mapper;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.order.domain.Order;
import kwh.cofshop.order.domain.OrderItem;
import kwh.cofshop.order.dto.request.OrderRequestDto;
import kwh.cofshop.order.dto.response.OrderItemResponseDto;
import kwh.cofshop.order.dto.response.OrderResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-02-15T14:55:54+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.9 (Oracle Corporation)"
)
@Component
public class OrderMapperImpl implements OrderMapper {

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Override
    public Order toEntity(OrderRequestDto requestDto, Member member) {
        if ( requestDto == null && member == null ) {
            return null;
        }

        Order.OrderBuilder order = Order.builder();

        order.member( member );

        return order.build();
    }

    @Override
    public OrderResponseDto toResponseDto(Order order) {
        if ( order == null ) {
            return null;
        }

        OrderResponseDto orderResponseDto = new OrderResponseDto();

        orderResponseDto.setOrdererResponseDto( toOrdererResponseDto( order ) );
        orderResponseDto.setOrderItemResponseDto( orderItemListToOrderItemResponseDtoList( order.getOrderItems() ) );
        orderResponseDto.setOrderState( order.getOrderState() );

        return orderResponseDto;
    }

    protected List<OrderItemResponseDto> orderItemListToOrderItemResponseDtoList(List<OrderItem> list) {
        if ( list == null ) {
            return null;
        }

        List<OrderItemResponseDto> list1 = new ArrayList<OrderItemResponseDto>( list.size() );
        for ( OrderItem orderItem : list ) {
            list1.add( orderItemMapper.toResponseDto( orderItem ) );
        }

        return list1;
    }
}
