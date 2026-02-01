package kwh.cofshop.item.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ItemUpdateResponseDto {
    private String itemName;
    private Integer price;
    private Integer discount;
    private Integer deliveryFee;
    private Long categoryId;
    private String origin;
    private Integer itemLimit;

    // 연관관계
    // 기존 데이터
    private List<ItemImgResponseDto> existingItemImgs; // 기존 이미지 목록
    private List<ItemOptionResponseDto> existingItemOptions; // 기존 옵션 목록
    private List<Long> existingCategoryIds; // 기존 카테고리 목록

    // 추가할 데이터
    private List<ItemImgResponseDto> addItemImgs; // 추가할 이미지
    private List<ItemOptionResponseDto> addItemOptions; // 추가할 옵션
    private List<Long> addCategoryIds; // 추가할 카테고리

    // 삭제할 데이터
    private List<Long> deleteImgIds; // 삭제할 이미지
    private List<Long> deleteOptionIds; // 삭제할 옵션
    private List<Long> deleteCategoryIds; // 삭제할 카테고리
}
