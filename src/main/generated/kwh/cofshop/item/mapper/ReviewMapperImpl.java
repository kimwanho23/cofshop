package kwh.cofshop.item.mapper;

import javax.annotation.processing.Generated;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.Review;
import kwh.cofshop.item.dto.response.ReviewResponseDto;
import kwh.cofshop.member.domain.Member;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-24T00:40:10+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.9 (Oracle Corporation)"
)
@Component
public class ReviewMapperImpl implements ReviewMapper {

    @Override
    public ReviewResponseDto toResponseDto(Review review) {
        if ( review == null ) {
            return null;
        }

        ReviewResponseDto reviewResponseDto = new ReviewResponseDto();

        reviewResponseDto.setItemId( reviewItemId( review ) );
        reviewResponseDto.setMemberId( reviewMemberId( review ) );
        reviewResponseDto.setReviewId( review.getId() );
        reviewResponseDto.setRating( review.getRating() );
        reviewResponseDto.setContent( review.getContent() );

        return reviewResponseDto;
    }

    private Long reviewItemId(Review review) {
        if ( review == null ) {
            return null;
        }
        Item item = review.getItem();
        if ( item == null ) {
            return null;
        }
        Long id = item.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private Long reviewMemberId(Review review) {
        if ( review == null ) {
            return null;
        }
        Member member = review.getMember();
        if ( member == null ) {
            return null;
        }
        Long id = member.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }
}
