package kwh.cofshop.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PaymentPrepareRequestDto {

    @NotBlank
    private String pgProvider;

    @NotBlank
    private String payMethod;
}
