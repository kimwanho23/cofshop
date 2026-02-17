package kwh.cofshop.member.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.member.dto.request.MemberRequestDto;
import kwh.cofshop.member.dto.response.MemberResponseDto;
import kwh.cofshop.member.event.MemberCreatedEvent;
import kwh.cofshop.member.event.MemberSessionInvalidatedEvent;
import kwh.cofshop.member.mapper.MemberMapper;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public MemberResponseDto signUp(MemberRequestDto memberDto) {
        if (memberRepository.findByEmail(memberDto.getEmail()).isPresent()) {
            throw new BusinessException(BusinessErrorCode.MEMBER_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(memberDto.getMemberPwd());

        Member member;
        try {
            member = memberRepository.save(Member.builder()
                    .email(memberDto.getEmail())
                    .memberName(memberDto.getMemberName())
                    .memberPwd(encodedPassword)
                    .tel(memberDto.getTel())
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(BusinessErrorCode.MEMBER_ALREADY_EXISTS);
        }

        eventPublisher.publishEvent(new MemberCreatedEvent(member.getId()));
        return memberMapper.toResponseDto(member);
    }

    public MemberResponseDto findMember(Long memberId) {
        return memberRepository.findMemberResponseById(memberId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.MEMBER_NOT_FOUND));
    }

    public List<MemberResponseDto> memberLists() {
        return memberRepository.findAllMemberResponses();
    }

    @Transactional
    public void changeMemberState(Long memberId, MemberState newState) {
        Member member = getMember(memberId);
        MemberState currentState = member.getMemberState();
        if (currentState == newState) {
            return;
        }
        member.changeMemberState(newState);
        eventPublisher.publishEvent(new MemberSessionInvalidatedEvent(memberId));
    }

    @Transactional
    public void updateMemberPassword(Long memberId, String newPassword) {
        Member member = getMember(memberId);
        member.changePassword(passwordEncoder.encode(newPassword));
        eventPublisher.publishEvent(new MemberSessionInvalidatedEvent(memberId));
    }

    @Transactional
    public Integer updatePoint(Long id, int point) {
        Member member = getMember(id);
        member.updatePoint(point);
        return member.getPoint();
    }

    public Member getMember(Long id) {
        return memberRepository.findById(id).orElseThrow(
                () -> new BusinessException(BusinessErrorCode.MEMBER_NOT_FOUND)
        );
    }
}
