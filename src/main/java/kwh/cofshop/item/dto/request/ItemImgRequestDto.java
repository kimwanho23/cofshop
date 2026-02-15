package kwh.cofshop.item.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import kwh.cofshop.item.domain.ImgType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemImgRequestDto {
    @Positive(message = "이미지 ID는 1 이상이어야 합니다.")
    private Long id;

    @Positive(message = "상품 ID는 1 이상이어야 합니다.")
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
