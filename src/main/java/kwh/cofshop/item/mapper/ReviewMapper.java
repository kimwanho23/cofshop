package kwh.cofshop.item.mapper;

import kwh.cofshop.item.domain.Review;
import kwh.cofshop.item.dto.response.ReviewResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;


@Mapper(
        componentModel = SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ReviewMapper {

    //  Review toEntity(ReviewRequestDto reviewRequestDto);

    @Mapping(source = "item.id", target = "itemId")
    @Mapping(source = "member.id", target = "memberId")
    @Mapping(source = "review.id", target = "reviewId")
    ReviewResponseDto toResponseDto(Review review);

}






