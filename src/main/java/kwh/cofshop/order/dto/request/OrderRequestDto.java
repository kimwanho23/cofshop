package kwh.cofshop.order.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderRequestDto {

    private OrdererRequestDto ordererRequestDto;             // 주문 정보
    private List<OrderItemRequestDto> orderItemRequestDtoList;  // 주문 항목 리스트
}
