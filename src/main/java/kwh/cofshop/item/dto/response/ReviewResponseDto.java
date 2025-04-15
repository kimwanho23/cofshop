package kwh.cofshop.item.dto.response;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewResponseDto {

    private Long reviewId;
    private Long memberId;
    private Long rating; // 별점
    private String content; // 후기글
    private Long itemId; // 상품
}
