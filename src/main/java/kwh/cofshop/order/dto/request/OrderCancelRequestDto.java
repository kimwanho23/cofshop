package kwh.cofshop.order.dto.request;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class OrderCancelRequestDto {

    @NotBlank(message = "취소 사유는 필수입니다.")
    private String cancelReason; // 취소 사유
}
