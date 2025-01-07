package kwh.cofshop.item.dto.response;

import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import kwh.cofshop.item.domain.ItemState;
import kwh.cofshop.item.dto.request.ItemImgRequestDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public class ItemResponseDto {
    private String itemName; // 상품명
    private Integer price; // 가격
    private Integer discount; // 할인율 (null 허용)
    private Integer deliveryFee; // 배송비 (null이면 무료)
    private String categories; // 상품 카테고리
    private String origin; // 원산지
    private Integer itemLimit; // 수량 제한(한 번에 구입 가능한 개수)
    private ItemState itemState;
    private String email; // 판매자 이메일

}