package kwh.cofshop.item.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ItemRequestDto {

    @NotBlank(message = "상품명은 필수입니다.")
    private String itemName; // 상품명

    @NotNull(message = "가격은 필수입니다.")
    @Positive(message = "가격은 0보다 커야 합니다.")
    private Integer price; // 가격

    @PositiveOrZero(message = "할인율은 0 이상이어야 합니다.")
    private Integer discount; // 할인율(null 가능)

    @PositiveOrZero(message = "배송비는 0 이상이어야 합니다.")
    private Integer deliveryFee; // 배송비(null이면 무료)

    private String origin; // 원산지

    @PositiveOrZero(message = "수량 제한은 0 이상이어야 합니다.")
    private Integer itemLimit; // 수량 제한(한번에 구매 가능한 개수)

    @NotEmpty(message = "카테고리 목록은 필수입니다.")
    private List<Long> categoryIds; // 카테고리 목록

    @Valid
    private List<ItemImgRequestDto> itemImgRequestDto; // 이미지

    @Valid
    private List<ItemOptionRequestDto> itemOptionRequestDto; // 옵션
}
