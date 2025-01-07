package kwh.cofshop.order.dto.response;

import kwh.cofshop.item.dto.response.ItemOptionResponseDto;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import kwh.cofshop.order.domain.OrderState;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemResponseDto {
    private ItemResponseDto item;
    private ItemOptionResponseDto itemOption;
    private Integer orderPrice; // 가격
    private Integer quantity;  // 수량
}
