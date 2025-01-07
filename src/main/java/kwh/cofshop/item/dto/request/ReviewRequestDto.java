package kwh.cofshop.item.dto.request;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequestDto {
    private Long rating; // 별점
    private String content; // 후기글
    private Long item; // 상품
    private String member; // 작성자
}
