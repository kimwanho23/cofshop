package kwh.cofshop.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentPrepareRequestDto {

    @NotBlank
    @Size(max = 50)
    private String pgProvider;

    @NotBlank
    @Size(max = 50)
    private String payMethod;

    public PaymentPrepareRequestDto(String pgProvider, String payMethod) {
        this.pgProvider = pgProvider;
        this.payMethod = payMethod;
    }
}
