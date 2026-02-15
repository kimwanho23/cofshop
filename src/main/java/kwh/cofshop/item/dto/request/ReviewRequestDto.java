package kwh.cofshop.item.dto.request;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequestDto {
    @NotNull(message = "별점은 필수입니다.")
    @Min(value = 1, message = "별점은 1 이상이어야 합니다.")
    @Max(value = 5, message = "별점은 5 이하이어야 합니다.")
    private Long rating; // 별점

    @NotBlank(message = "후기 내용은 필수입니다.")
    @Size(max = 2000, message = "후기 내용은 2000자 이하여야 합니다.")
    private String content; // 후기글
}
