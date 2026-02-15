package kwh.cofshop.order.dto.request;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@Setter
public class OrderCancelRequestDto {

    @NotBlank(message = "취소 사유는 필수입니다.")
    @Size(max = 500, message = "취소 사유는 500자 이하여야 합니다.")
    private String cancelReason; // 취소 사유
}
