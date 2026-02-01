package kwh.cofshop.security;

import kwh.cofshop.global.exception.UnauthorizedRequestException;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.member.domain.Role;
import kwh.cofshop.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("사용자 조회 실패")
    void loadUserByUsername_notFound() {
        when(memberRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("user@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("정지 회원")
    void loadUserByUsername_suspended() {
        Member member = createMember(MemberState.SUSPENDED);
        when(memberRepository.findByEmail("user@example.com")).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("user@example.com"))
                .isInstanceOf(UnauthorizedRequestException.class);
    }

    @Test
    @DisplayName("탈퇴 회원")
    void loadUserByUsername_quit() {
        Member member = createMember(MemberState.QUIT);
        when(memberRepository.findByEmail("user@example.com")).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("user@example.com"))
                .isInstanceOf(UnauthorizedRequestException.class);
    }

    @Test
    @DisplayName("정상 회원")
    void loadUserByUsername_success() {
        Member member = createMember(MemberState.ACTIVE);
        when(memberRepository.findByEmail("user@example.com")).thenReturn(Optional.of(member));

        CustomUserDetails result = (CustomUserDetails) customUserDetailsService.loadUserByUsername("user@example.com");

        assertThat(result.getEmail()).isEqualTo("user@example.com");
        assertThat(result.getAuthorities()).isNotEmpty();
    }

    private Member createMember(MemberState state) {
        Member member = Member.builder()
                .id(1L)
                .email("user@example.com")
                .memberName("사용자")
                .memberPwd("pw")
                .tel("01012341234")
                .build();
        ReflectionTestUtils.setField(member, "memberState", state);
        ReflectionTestUtils.setField(member, "role", Role.MEMBER);
        ReflectionTestUtils.setField(member, "lastPasswordChange", LocalDateTime.now());
        return member;
    }
}