package kwh.cofshop.member.service;

import kwh.cofshop.member.event.MemberCreatedEvent;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.member.dto.request.MemberRequestDto;
import kwh.cofshop.member.dto.response.MemberResponseDto;
import kwh.cofshop.member.mapper.MemberMapper;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;


    @Transactional
    public MemberResponseDto signUp(MemberRequestDto memberDto){ // Save, Update 로직
        if (memberRepository.findByEmail(memberDto.getEmail()).isPresent()){
            throw new BusinessException(BusinessErrorCode.MEMBER_ALREADY_EXISTS); // 이미 존재하는 이메일
        }

        memberDto.setMemberPwd(passwordEncoder.encode(memberDto.getMemberPwd()));

        Member member = memberRepository.save(memberMapper.toEntity(memberDto));
        eventPublisher.publishEvent(new MemberCreatedEvent(member.getId()));
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
    public void changeMemberState(Long memberId, MemberState newState) {
        Member member = getMember(memberId);
        member.changeMemberState(newState);
    }

    // 비밀번호 변경
    @Transactional
    public void updateMemberPassword(Long memberId, String newPassword) {
        Member member = getMember(memberId);
        String encodePassword = passwordEncoder.encode(newPassword);
        member.changePassword(encodePassword);
    }

    // 포인트 증가
    @Transactional
    public Integer updatePoint(Long id, int point) {
        Member member = getMember(id);
        member.updatePoint(point);
        return member.getPoint();
    }


    // 포인트 복구
    @Transactional
    public void restorePoint(Long memberId, int point) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow();
        member.restorePoint(point);
    }

    public Member getMember(Long id) {
        return memberRepository.findById(id).orElseThrow(
                () -> new BusinessException(BusinessErrorCode.MEMBER_NOT_FOUND)
        );
    }
}
