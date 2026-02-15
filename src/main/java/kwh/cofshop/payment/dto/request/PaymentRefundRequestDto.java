package kwh.cofshop.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRefundRequestDto {
    @NotNull
    @Positive
    private Long amount;
}
