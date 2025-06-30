package kwh.cofshop.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentVerifyRequestDto {
    @NotNull
    private String impUid;

    @NotNull
    private String merchantUid;

    @NotNull
    private Long amount; // 백엔드와 일치하는지 검증
}
