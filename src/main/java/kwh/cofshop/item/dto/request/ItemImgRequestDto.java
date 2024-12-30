package kwh.cofshop.item.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class ItemImgRequestDto {

    private Long itemId;       // 연관된 상품 ID
    private MultipartFile repImage; // 대표 이미지 파일
    private List<MultipartFile> subImages; // 서브 이미지 파일 목록
}
