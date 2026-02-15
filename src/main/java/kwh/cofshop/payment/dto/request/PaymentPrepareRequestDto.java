package kwh.cofshop.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PaymentPrepareRequestDto {

    @NotBlank
    @Size(max = 50)
    private String pgProvider;

    @NotBlank
    @Size(max = 50)
    private String payMethod;
}
