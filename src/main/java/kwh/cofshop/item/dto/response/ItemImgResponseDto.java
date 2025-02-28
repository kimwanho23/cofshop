package kwh.cofshop.item.dto.response;

import kwh.cofshop.item.domain.ImgType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemImgResponseDto {
    private Long id; // 이미지 ID
    private String imgName;    // 저장된 이미지 이름
    private String oriImgName; // 원본 이미지 이름
    private String imgUrl;     // 이미지 URL
    private ImgType imgType;     // 대표 이미지 여부
}
