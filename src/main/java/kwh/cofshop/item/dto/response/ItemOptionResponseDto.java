package kwh.cofshop.item.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemOptionResponseDto {

    private String description; // 옵션 내용
    private Integer additionalPrice; // 추가금 (기본금에 더해서)
    private Integer stock; // 옵션별 재고
}
