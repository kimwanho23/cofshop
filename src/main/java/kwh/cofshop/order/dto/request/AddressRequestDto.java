package kwh.cofshop.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequestDto {

    @NotBlank(message = "도시는 필수입니다.")
    @Size(max = 100, message = "도시는 100자 이하여야 합니다.")
    private String city;

    @NotBlank(message = "상세 주소는 필수입니다.")
    @Size(max = 255, message = "상세 주소는 255자 이하여야 합니다.")
    private String street;

    @NotBlank(message = "우편번호는 필수입니다.")
    @Size(max = 20, message = "우편번호는 20자 이하여야 합니다.")
    private String zipCode;
}
