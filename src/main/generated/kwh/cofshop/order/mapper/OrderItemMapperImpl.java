package kwh.cofshop.order.mapper;

import javax.annotation.processing.Generated;
import kwh.cofshop.item.domain.Category;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.order.domain.OrderItem;
import kwh.cofshop.order.dto.response.OrderItemOptionResponseDto;
import kwh.cofshop.order.dto.response.OrderItemResponseDto;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-02-15T13:59:40+0900",
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
        orderItemResponseDto.setDiscount( orderItemItemDiscount( orderItem ) );
        orderItemResponseDto.setDeliveryFee( orderItemItemDeliveryFee( orderItem ) );
        orderItemResponseDto.setCategories( orderItemItemCategory( orderItem ) );
        orderItemResponseDto.setOrigin( orderItemItemOrigin( orderItem ) );
        orderItemResponseDto.setQuantity( orderItem.getQuantity() );
        orderItemResponseDto.setItemOption( itemOptionToOrderItemOptionResponseDto( orderItem.getItemOption() ) );

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

    private Integer orderItemItemDiscount(OrderItem orderItem) {
        if ( orderItem == null ) {
            return null;
        }
        Item item = orderItem.getItem();
        if ( item == null ) {
            return null;
        }
        Integer discount = item.getDiscount();
        if ( discount == null ) {
            return null;
        }
        return discount;
    }

    private Integer orderItemItemDeliveryFee(OrderItem orderItem) {
        if ( orderItem == null ) {
            return null;
        }
        Item item = orderItem.getItem();
        if ( item == null ) {
            return null;
        }
        Integer deliveryFee = item.getDeliveryFee();
        if ( deliveryFee == null ) {
            return null;
        }
        return deliveryFee;
    }

    private Category orderItemItemCategory(OrderItem orderItem) {
        if ( orderItem == null ) {
            return null;
        }
        Item item = orderItem.getItem();
        if ( item == null ) {
            return null;
        }
        Category category = item.getCategory();
        if ( category == null ) {
            return null;
        }
        return category;
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

    protected OrderItemOptionResponseDto itemOptionToOrderItemOptionResponseDto(ItemOption itemOption) {
        if ( itemOption == null ) {
            return null;
        }

        OrderItemOptionResponseDto orderItemOptionResponseDto = new OrderItemOptionResponseDto();

        orderItemOptionResponseDto.setDescription( itemOption.getDescription() );
        orderItemOptionResponseDto.setAdditionalPrice( itemOption.getAdditionalPrice() );

        return orderItemOptionResponseDto;
    }
}
