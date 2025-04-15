package kwh.cofshop.member.mapper;

import kwh.cofshop.member.domain.MemberLoginHistory;
import kwh.cofshop.member.event.MemberLoginEvent;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MemberLoginHistoryMapper {

    MemberLoginHistory toEntity(MemberLoginEvent memberLoginHistoryDto);

    MemberLoginEvent toResponseDto(MemberLoginHistory memberLoginHistory);
}
