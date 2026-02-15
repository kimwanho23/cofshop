package kwh.cofshop.order.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressResponseDto {
    private String city;
    private String street;
    private String zipCode;
}
