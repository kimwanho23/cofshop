package kwh.cofshop.item.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ItemUpdateRequestDto {
    private String itemName;

    @PositiveOrZero(message = "가격은 0 이상이어야 합니다.")
    private Integer price;

    @PositiveOrZero(message = "할인율은 0 이상이어야 합니다.")
    private Integer discount;

    @PositiveOrZero(message = "배송비는 0 이상이어야 합니다.")
    private Integer deliveryFee;

    private String origin;

    @PositiveOrZero(message = "수량 제한은 0 이상이어야 합니다.")
    private Integer itemLimit;

    // 연관관계
    // 기존 데이터
    @Valid
    private List<ItemImgRequestDto> existingItemImgs; // 기존 이미지 목록

    @Valid
    private List<ItemOptionRequestDto> existingItemOptions; // 기존 옵션 목록

    private List<Long> existingCategoryIds; // 기존 카테고리 목록

    // 추가할 데이터
    @Valid
    private List<ItemImgRequestDto> addItemImgs; // 추가할 이미지

    @Valid
    private List<ItemOptionRequestDto> addItemOptions; // 추가할 옵션

    private List<Long> addCategoryIds; // 추가할 카테고리

    // 삭제할 데이터
    private List<Long> deleteImgIds; // 삭제할 이미지
    private List<Long> deleteOptionIds; // 삭제할 옵션
    private List<Long> deleteCategoryIds; // 삭제할 카테고리
}
