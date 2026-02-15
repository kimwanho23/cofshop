package kwh.cofshop.member.mapper;


import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.dto.request.MemberRequestDto;
import kwh.cofshop.member.dto.response.MemberResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MemberMapper {
    // DTO → 엔티티 매핑
    Member toEntity(MemberRequestDto dto);

    // 엔티티 → DTO 매핑
    @Mapping(target = "memberId", source = "id")
    MemberResponseDto toResponseDto(Member member);

}
