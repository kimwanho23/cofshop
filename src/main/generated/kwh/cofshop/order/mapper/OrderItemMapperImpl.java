package kwh.cofshop.order.mapper;

import javax.annotation.processing.Generated;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.order.domain.OrderItem;
import kwh.cofshop.order.dto.response.OrderItemResponseDto;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-24T00:40:10+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.9 (Oracle Corporation)"
)
@Component
public class OrderItemMapperImpl implements OrderItemMapper {

    @Override
    public OrderItemResponseDto toResponseDto(OrderItem orderItem) {
        if ( orderItem == null ) {
            return null;
        }

        OrderItemResponseDto orderItemResponseDto = new OrderItemResponseDto();

        orderItemResponseDto.setItemName( orderItemItemItemName( orderItem ) );
        orderItemResponseDto.setPrice( orderItemItemPrice( orderItem ) );
        orderItemResponseDto.setAdditionalPrice( orderItemItemOptionAdditionalPrice( orderItem ) );
        orderItemResponseDto.setOrigin( orderItemItemOrigin( orderItem ) );
        orderItemResponseDto.setDiscountRate( orderItemItemOptionDiscountRate( orderItem ) );
        orderItemResponseDto.setQuantity( orderItem.getQuantity() );

        return orderItemResponseDto;
    }

    private String orderItemItemItemName(OrderItem orderItem) {
        if ( orderItem == null ) {
            return null;
        }
        Item item = orderItem.getItem();
        if ( item == null ) {
            return null;
        }
        String itemName = item.getItemName();
        if ( itemName == null ) {
            return null;
        }
        return itemName;
    }

    private Integer orderItemItemPrice(OrderItem orderItem) {
        if ( orderItem == null ) {
            return null;
        }
        Item item = orderItem.getItem();
        if ( item == null ) {
            return null;
        }
        int price = item.getPrice();
        return price;
    }

    private Integer orderItemItemOptionAdditionalPrice(OrderItem orderItem) {
        if ( orderItem == null ) {
            return null;
        }
        ItemOption itemOption = orderItem.getItemOption();
        if ( itemOption == null ) {
            return null;
        }
        Integer additionalPrice = itemOption.getAdditionalPrice();
        if ( additionalPrice == null ) {
            return null;
        }
        return additionalPrice;
    }

    private String orderItemItemOrigin(OrderItem orderItem) {
        if ( orderItem == null ) {
            return null;
        }
        Item item = orderItem.getItem();
        if ( item == null ) {
            return null;
        }
        String origin = item.getOrigin();
        if ( origin == null ) {
            return null;
        }
        return origin;
    }

    private Integer orderItemItemOptionDiscountRate(OrderItem orderItem) {
        if ( orderItem == null ) {
            return null;
        }
        ItemOption itemOption = orderItem.getItemOption();
        if ( itemOption == null ) {
            return null;
        }
        Integer discountRate = itemOption.getDiscountRate();
        if ( discountRate == null ) {
            return null;
        }
        return discountRate;
    }
}
