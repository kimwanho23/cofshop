package kwh.cofshop.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PaymentPrepareRequestDto {

    @NotNull
    private String pgProvider;

    @NotNull
    private String payMethod;

    @NotNull
    private Long totalPrice;
}
