package kwh.cofshop.item.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ItemCreateResponseDto {

    private ItemResponseDto itemResponseDto;
    private List<ItemImgResponseDto> imgResponseDto; // 이미지 정보
    private List<ItemOptionResponseDto> optionResponseDto; // 옵션 목록
}
