package kwh.cofshop.member.service;

import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.global.exception.errorcodes.ErrorCode;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.dto.LoginDto;
import kwh.cofshop.member.dto.MemberRequestDto;
import kwh.cofshop.member.dto.MemberResponseDto;
import kwh.cofshop.member.mapper.MemberMapper;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public Member save(MemberRequestDto memberDto){ // Save, Update 로직
        memberDto.setMemberPwd(passwordEncoder.encode(memberDto.getMemberPwd()));
        return memberRepository.save(memberMapper.toEntity(memberDto)); // DTO 엔티티로 변환해서 저장
    }

    public MemberResponseDto findMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BadRequestException(BadRequestErrorCode.USER_NOT_FOUND));

        return memberMapper.toResponseDto(member); //엔티티를 DTO로 변환, 엔티티를 받아오더라도 DTO로 변환하여 엔티티의 직접적인 변경을 막는다.
    }

    public MemberResponseDto findMember(String email) {
        Member member = memberRepository.findByEmail(email).orElseThrow();
        return memberMapper.toResponseDto(member); //엔티티를 DTO로 변환, 엔티티를 받아오더라도 DTO로 변환하여 엔티티의 직접적인 변경을 막는다.
    }

    public List<MemberResponseDto> memberLists() {
        return memberRepository.findAll().stream()
                .map(memberMapper::toResponseDto)
                .toList();
    }

/*    public Member login(LoginDto loginDto) {
        Member findMember = memberRepository.findByEmail(loginDto.getEmail()).orElseThrow();

        if (!passwordEncoder.matches(loginDto.getMemberPwd(), findMember.getMemberPwd())) {
            throw new BadCredentialsException("비밀번호가 잘못되었습니다.");
        }
        return findMember;
    }*/
}
