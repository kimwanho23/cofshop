package kwh.cofshop.item.dto.request;

import jakarta.validation.constraints.NotNull;
import kwh.cofshop.item.domain.ImgType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemImgRequestDto {
    private Long id;
    private Long itemId;

    @NotNull(message = "이미지 타입은 필수입니다.")
    private ImgType imgType;

    public ItemImgRequestDto() {
    }

    public ItemImgRequestDto(Long id, Long itemId, ImgType imgType) {
        this.id = id;
        this.itemId = itemId;
        this.imgType = imgType;
    }
}
