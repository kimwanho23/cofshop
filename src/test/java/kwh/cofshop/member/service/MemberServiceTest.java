package kwh.cofshop.member.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.member.dto.request.MemberRequestDto;
import kwh.cofshop.member.dto.response.MemberResponseDto;
import kwh.cofshop.member.event.MemberCreatedEvent;
import kwh.cofshop.member.mapper.MemberMapper;
import kwh.cofshop.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("회원 가입: 중복 이메일")
    void signUp_duplicateEmail() {
        MemberRequestDto requestDto = new MemberRequestDto();
        requestDto.setEmail("user@example.com");

        when(memberRepository.findByEmail("user@example.com")).thenReturn(Optional.of(Member.builder().build()));

        assertThatThrownBy(() -> memberService.signUp(requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("회원 가입: 성공")
    void signUp_success() {
        MemberRequestDto requestDto = new MemberRequestDto();
        requestDto.setEmail("user@example.com");
        requestDto.setMemberName("사용자");
        requestDto.setMemberPwd("pw");
        requestDto.setTel("01012341234");

        Member member = Member.builder()
                .id(1L)
                .email("user@example.com")
                .memberName("사용자")
                .memberPwd("encoded")
                .tel("01012341234")
                .build();
        MemberResponseDto responseDto = new MemberResponseDto();

        when(memberRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pw")).thenReturn("encoded");
        when(memberMapper.toEntity(any(MemberRequestDto.class))).thenReturn(member);
        when(memberRepository.save(member)).thenReturn(member);
        when(memberMapper.toResponseDto(member)).thenReturn(responseDto);

        MemberResponseDto result = memberService.signUp(requestDto);

        assertThat(result).isSameAs(responseDto);
        ArgumentCaptor<MemberCreatedEvent> captor = ArgumentCaptor.forClass(MemberCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().memberId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("회원 단건 조회")
    void findMember() {
        Member member = Member.builder().build();
        MemberResponseDto responseDto = new MemberResponseDto();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(memberMapper.toResponseDto(member)).thenReturn(responseDto);

        MemberResponseDto result = memberService.findMember(1L);

        assertThat(result).isSameAs(responseDto);
    }

    @Test
    @DisplayName("회원 목록 조회")
    void memberLists() {
        Member member = Member.builder().build();
        when(memberRepository.findAll()).thenReturn(List.of(member));
        when(memberMapper.toResponseDto(member)).thenReturn(new MemberResponseDto());

        List<MemberResponseDto> results = memberService.memberLists();

        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("회원 상태 변경")
    void changeMemberState() {
        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "memberState", MemberState.ACTIVE);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        memberService.changeMemberState(1L, MemberState.SUSPENDED);

        assertThat(member.getMemberState()).isEqualTo(MemberState.SUSPENDED);
    }

    @Test
    @DisplayName("비밀번호 변경")
    void updateMemberPassword() {
        Member member = Member.builder().build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(passwordEncoder.encode("new")).thenReturn("encoded");

        memberService.updateMemberPassword(1L, "new");

        assertThat(member.getMemberPwd()).isEqualTo("encoded");
    }

    @Test
    @DisplayName("포인트 변경")
    void updatePoint() {
        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "point", 0);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        Integer result = memberService.updatePoint(1L, 100);

        assertThat(result).isEqualTo(100);
    }

    @Test
    @DisplayName("포인트 복구: 성공")
    void restorePoint_success() {
        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "point", 0);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        memberService.restorePoint(1L, 100);

        assertThat(member.getPoint()).isEqualTo(100);
    }

    @Test
    @DisplayName("포인트 복구: 실패")
    void restorePoint_invalid() {
        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "point", 0);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.restorePoint(1L, 0))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(BusinessErrorCode.INVALID_POINT_OPERATION);
    }

    @Test
    @DisplayName("회원 조회 실패")
    void getMember_notFound() {
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMember(1L))
                .isInstanceOf(BusinessException.class);
    }
}
