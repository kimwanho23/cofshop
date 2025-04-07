package kwh.cofshop.order.dto.request;

import kwh.cofshop.order.domain.Address;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderRequestDto {

    private Address address; // 주문
    private List<OrderItemRequestDto> orderItemRequestDtoList;  // 주문 항목 리스트
}
