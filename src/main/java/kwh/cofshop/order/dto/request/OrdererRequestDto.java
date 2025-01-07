package kwh.cofshop.order.dto.request;

import kwh.cofshop.order.domain.Address;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class OrdererRequestDto { // 주문자 기본 정보
    private String email; // 주문자
    private Address address; // 배송지
}
