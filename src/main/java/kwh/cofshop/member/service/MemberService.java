package kwh.cofshop.member.service;

import kwh.cofshop.security.domain.RefreshToken;
import kwh.cofshop.security.dto.TokenDto;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.member.dto.request.LoginDto;
import kwh.cofshop.member.dto.request.MemberRequestDto;
import kwh.cofshop.member.dto.response.LoginResponseDto;
import kwh.cofshop.member.dto.response.MemberResponseDto;
import kwh.cofshop.member.mapper.MemberMapper;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.security.CustomUserDetails;
import kwh.cofshop.security.JwtTokenProvider;
import kwh.cofshop.security.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

    private final MemberRepository memberRepository;

    private final MemberMapper memberMapper;
    private final BCryptPasswordEncoder passwordEncoder;


    @Transactional
    public MemberResponseDto save(MemberRequestDto memberDto){ // Save, Update 로직
        if (memberRepository.findByEmail(memberDto.getEmail()).isPresent()){
            throw new BusinessException(BusinessErrorCode.MEMBER_ALREADY_EXIST); // 이미 존재하는 이메일
        }

        memberDto.setMemberPwd(passwordEncoder.encode(memberDto.getMemberPwd()));

        Member member = memberRepository.save(memberMapper.toEntity(memberDto));// DTO 엔티티로 변환해서 저장
        return memberMapper.toResponseDto(member);
    }

    public MemberResponseDto findMember(Long memberId) {
        Member member = getMember(memberId);
        return memberMapper.toResponseDto(member);
    }

    public List<MemberResponseDto> memberLists() {
        return memberRepository.findAll().stream()
                .map(memberMapper::toResponseDto)
                .toList();
    }

    // 회원 상태 변경
    @Transactional
    public void changeMemberState(Long id, MemberState newState) {
        Member member = getMember(id);
        member.changeMemberState(newState);
    }

    // 비밀번호 변경
    @Transactional
    public void updateMemberPassword(Long id, String newPassword) {
        Member member = getMember(id);
        String encodePassword = passwordEncoder.encode(newPassword);
        member.changePassword(encodePassword);
    }


    @Transactional
    public Integer updatePoint(Long id, int amount) { // 포인트 증가
        Member member = getMember(id);
        member.updatePoint(amount);
        return member.getPoint();
    }

    private Member getMember(Long id) {
        return memberRepository.findById(id).orElseThrow(
                () -> new BusinessException(BusinessErrorCode.MEMBER_NOT_FOUND)
        );
    }
}
