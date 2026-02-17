package kwh.cofshop.member.service;

import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.member.dto.request.MemberRequestDto;
import kwh.cofshop.member.dto.response.MemberResponseDto;
import kwh.cofshop.member.event.MemberCreatedEvent;
import kwh.cofshop.member.event.MemberSessionInvalidatedEvent;
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
import static org.mockito.ArgumentMatchers.eq;
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
    @DisplayName("Sign up fails when email already exists")
    void signUp_duplicateEmail() {
        MemberRequestDto requestDto = new MemberRequestDto();
        requestDto.setEmail("user@example.com");

        when(memberRepository.findByEmail("user@example.com")).thenReturn(Optional.of(Member.builder().build()));

        assertThatThrownBy(() -> memberService.signUp(requestDto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Sign up succeeds")
    void signUp_success() {
        MemberRequestDto requestDto = new MemberRequestDto();
        requestDto.setEmail("user@example.com");
        requestDto.setMemberName("user");
        requestDto.setMemberPwd("pw");
        requestDto.setTel("01012341234");

        Member member = Member.builder()
                .id(1L)
                .email("user@example.com")
                .memberName("user")
                .memberPwd("encoded")
                .tel("01012341234")
                .build();
        MemberResponseDto responseDto = new MemberResponseDto();

        when(memberRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pw")).thenReturn("encoded");
        when(memberRepository.save(any(Member.class))).thenReturn(member);
        when(memberMapper.toResponseDto(member)).thenReturn(responseDto);

        MemberResponseDto result = memberService.signUp(requestDto);

        assertThat(result).isSameAs(responseDto);
        verify(passwordEncoder).encode(eq("pw"));
        ArgumentCaptor<MemberCreatedEvent> captor = ArgumentCaptor.forClass(MemberCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().memberId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Find member by id")
    void findMember() {
        MemberResponseDto responseDto = new MemberResponseDto();
        when(memberRepository.findMemberResponseById(1L)).thenReturn(Optional.of(responseDto));

        MemberResponseDto result = memberService.findMember(1L);

        assertThat(result).isSameAs(responseDto);
    }

    @Test
    @DisplayName("Find member list")
    void memberLists() {
        when(memberRepository.findAllMemberResponses()).thenReturn(List.of(new MemberResponseDto()));

        List<MemberResponseDto> results = memberService.memberLists();

        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("Change member state")
    void changeMemberState() {
        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "memberState", MemberState.ACTIVE);
        ReflectionTestUtils.setField(member, "id", 1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        memberService.changeMemberState(1L, MemberState.SUSPENDED);

        assertThat(member.getMemberState()).isEqualTo(MemberState.SUSPENDED);
        ArgumentCaptor<MemberSessionInvalidatedEvent> captor =
                ArgumentCaptor.forClass(MemberSessionInvalidatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().memberId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Update password")
    void updateMemberPassword() {
        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", 1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(passwordEncoder.encode("new")).thenReturn("encoded");

        memberService.updateMemberPassword(1L, "new");

        assertThat(member.getMemberPwd()).isEqualTo("encoded");
        ArgumentCaptor<MemberSessionInvalidatedEvent> captor =
                ArgumentCaptor.forClass(MemberSessionInvalidatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().memberId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Update point")
    void updatePoint() {
        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "point", 0);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        Integer result = memberService.updatePoint(1L, 100);

        assertThat(result).isEqualTo(100);
    }

    @Test
    @DisplayName("Get member fails when not found")
    void getMember_notFound() {
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMember(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Find member fails when not found")
    void findMember_notFound() {
        when(memberRepository.findMemberResponseById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.findMember(1L))
                .isInstanceOf(BusinessException.class);
    }
}
