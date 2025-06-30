package kwh.cofshop.item.vo;

import kwh.cofshop.item.dto.request.ItemImgRequestDto;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

@Getter
public class ItemImgUploadVO {
    private final ItemImgRequestDto imgRequestDto;
    private final MultipartFile multipartFile;

    public ItemImgUploadVO(ItemImgRequestDto imgRequestDto, MultipartFile multipartFile) {
        this.imgRequestDto = imgRequestDto;
        this.multipartFile = multipartFile;
    }

}
