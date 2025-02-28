package kwh.cofshop.member.mapper;

import javax.annotation.processing.Generated;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.dto.MemberRequestDto;
import kwh.cofshop.member.dto.MemberResponseDto;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-02-15T13:59:40+0900",
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

        MemberResponseDto memberResponseDto = new MemberResponseDto();

        memberResponseDto.setEmail( member.getEmail() );
        memberResponseDto.setMemberName( member.getMemberName() );
        memberResponseDto.setTel( member.getTel() );
        memberResponseDto.setRole( member.getRole() );
        memberResponseDto.setMemberState( member.getMemberState() );
        memberResponseDto.setPoint( member.getPoint() );
        memberResponseDto.setCreatedAt( member.getCreatedAt() );
        memberResponseDto.setLastPasswordChange( member.getLastPasswordChange() );
        memberResponseDto.setLastLogin( member.getLastLogin() );

        return memberResponseDto;
    }
}
