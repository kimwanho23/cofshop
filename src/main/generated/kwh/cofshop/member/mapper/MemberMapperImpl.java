package kwh.cofshop.member.mapper;

import javax.annotation.processing.Generated;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.dto.request.MemberRequestDto;
import kwh.cofshop.member.dto.response.MemberResponseDto;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-24T00:40:11+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.9 (Oracle Corporation)"
)
@Component
public class MemberMapperImpl implements MemberMapper {

    @Override
    public Member toEntity(MemberRequestDto dto) {
        if ( dto == null ) {
            return null;
        }

        Member.MemberBuilder member = Member.builder();

        member.email( dto.getEmail() );
        member.memberName( dto.getMemberName() );
        member.memberPwd( dto.getMemberPwd() );
        member.tel( dto.getTel() );

        return member.build();
    }

    @Override
    public MemberResponseDto toResponseDto(Member member) {
        if ( member == null ) {
            return null;
        }

        MemberResponseDto.MemberResponseDtoBuilder memberResponseDto = MemberResponseDto.builder();

        memberResponseDto.email( member.getEmail() );
        memberResponseDto.memberName( member.getMemberName() );
        memberResponseDto.tel( member.getTel() );
        memberResponseDto.role( member.getRole() );
        memberResponseDto.memberState( member.getMemberState() );
        memberResponseDto.point( member.getPoint() );
        memberResponseDto.createdAt( member.getCreatedAt() );
        memberResponseDto.lastPasswordChange( member.getLastPasswordChange() );
        memberResponseDto.lastLogin( member.getLastLogin() );

        return memberResponseDto.build();
    }
}
