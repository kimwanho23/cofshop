package kwh.cofshop.member.mapper;

import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.dto.response.MemberResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class MemberMapperTest {

    private final MemberMapper memberMapper = Mappers.getMapper(MemberMapper.class);

    @Test
    @DisplayName("Member.id는 MemberResponseDto.memberId로 매핑된다")
    void toResponseDto_mapsMemberId() {
        Member member = Member.builder()
                .id(11L)
                .email("user@example.com")
                .memberName("tester")
                .memberPwd("pw")
                .tel("01012341234")
                .build();

        MemberResponseDto responseDto = memberMapper.toResponseDto(member);

        assertThat(responseDto.getMemberId()).isEqualTo(11L);
    }
}
