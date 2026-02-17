package kwh.cofshop.member.repository.custom;

import kwh.cofshop.member.dto.response.MemberResponseDto;

import java.util.List;
import java.util.Optional;

public interface MemberRepositoryCustom {

    Optional<MemberResponseDto> findMemberResponseById(Long memberId);

    List<MemberResponseDto> findAllMemberResponses();
}
