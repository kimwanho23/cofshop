package kwh.cofshop.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import kwh.cofshop.payment.validation.SupportedPaymentMethod;
import kwh.cofshop.payment.validation.SupportedPgProvider;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentPrepareRequestDto {

    @NotBlank
    @Size(max = 50)
    @SupportedPgProvider
    private String pgProvider;

    @NotBlank
    @Size(max = 50)
    @SupportedPaymentMethod
    private String payMethod;

    public PaymentPrepareRequestDto(String pgProvider, String payMethod) {
        this.pgProvider = pgProvider;
        this.payMethod = payMethod;
    }
}
