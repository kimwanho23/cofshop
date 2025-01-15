package kwh.cofshop.item.mapper;

import kwh.cofshop.item.domain.Review;
import kwh.cofshop.item.dto.response.ReviewResponseDto;
import kwh.cofshop.member.domain.Member;
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

    @Mapping(source = "item.id", target = "item")
    @Mapping(source = "member", target = "member")
    ReviewResponseDto toResponseDto(Review review);


    default String map(Member member) {
        return member != null ? member.getEmail() : null;
    }

}






