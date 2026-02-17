package kwh.cofshop.item.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ItemUpdateRequestDto {
    @Size(max = 100, message = "상품명은 100자 이하여야 합니다.")
    private String itemName;

    @PositiveOrZero(message = "가격은 0 이상이어야 합니다.")
    private Integer price;

    @PositiveOrZero(message = "할인율은 0 이상이어야 합니다.")
    @Max(value = 100, message = "할인율은 100 이하여야 합니다.")
    private Integer discount;

    @PositiveOrZero(message = "배송비는 0 이상이어야 합니다.")
    private Integer deliveryFee;

    @Size(max = 100, message = "원산지는 100자 이하여야 합니다.")
    private String origin;

    @PositiveOrZero(message = "수량 제한은 0 이상이어야 합니다.")
    private Integer itemLimit;

    // 연관관계
    // 기존 데이터
    @Valid
    private List<ItemOptionRequestDto> existingItemOptions; // 기존 옵션 목록

    // 추가할 데이터
    @Valid
    private List<ItemImgRequestDto> addItemImgs; // 추가할 이미지

    @Valid
    private List<ItemOptionRequestDto> addItemOptions; // 추가할 옵션

    private List<@NotNull(message = "카테고리 ID는 필수입니다.") @Positive(message = "카테고리 ID는 1 이상이어야 합니다.") Long> addCategoryIds; // 추가할 카테고리

    // 삭제할 데이터
    private List<@NotNull(message = "이미지 ID는 필수입니다.") @Positive(message = "이미지 ID는 1 이상이어야 합니다.") Long> deleteImgIds; // 삭제할 이미지
    private List<@NotNull(message = "옵션 ID는 필수입니다.") @Positive(message = "옵션 ID는 1 이상이어야 합니다.") Long> deleteOptionIds; // 삭제할 옵션
    private List<@NotNull(message = "카테고리 ID는 필수입니다.") @Positive(message = "카테고리 ID는 1 이상이어야 합니다.") Long> deleteCategoryIds; // 삭제할 카테고리
}
