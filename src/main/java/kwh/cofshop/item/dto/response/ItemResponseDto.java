package kwh.cofshop.item.dto.response;

import kwh.cofshop.item.domain.ItemState;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ItemResponseDto {
    private Long id;
    private String itemName; // 상품명
    private Integer price; // 가격
    private Integer discount; // 할인율(null 가능)
    private Integer deliveryFee; // 배송비(null이면 무료)
    private Long categoryId; // 상품 카테고리
    private String origin; // 원산지
    private Integer itemLimit; // 수량 제한(한번에 구매 가능한 개수)
    private ItemState itemState; // 아이템 상태
    private String email; // 판매자 이메일

    private List<String> categoryNames; // 카테고리
    private List<ItemImgResponseDto> imgResponseDto; // 이미지 정보
    private List<ItemOptionResponseDto> optionResponseDto; // 옵션 목록
}
