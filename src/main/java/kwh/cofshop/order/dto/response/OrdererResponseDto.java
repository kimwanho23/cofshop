package kwh.cofshop.order.dto.response;


import kwh.cofshop.item.domain.ItemState;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.order.domain.Address;
import kwh.cofshop.order.domain.OrderItem;
import kwh.cofshop.order.domain.OrderState;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class OrdererResponseDto {
    private String email; // 주문자
    private Address address;  // 주소지
}
