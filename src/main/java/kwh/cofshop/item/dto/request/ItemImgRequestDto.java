package kwh.cofshop.item.dto.request;

import kwh.cofshop.item.domain.ImgType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ItemImgRequestDto {

    private Long itemId;
    private ImgType imgType;
}
