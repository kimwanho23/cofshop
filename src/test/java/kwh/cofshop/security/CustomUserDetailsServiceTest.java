package kwh.cofshop.security;

import kwh.cofshop.global.exception.UnauthorizedRequestException;
import kwh.cofshop.member.api.MemberReadPort;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.member.domain.Role;
import kwh.cofshop.security.userdetails.CustomUserDetails;
import kwh.cofshop.security.userdetails.CustomUserDetailsService;
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
    private MemberReadPort memberReadPort;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("load user not found")
    void loadUserByUsername_notFound() {
        when(memberReadPort.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("user@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("load user suspended")
    void loadUserByUsername_suspended() {
        Member member = createMember(MemberState.SUSPENDED);
        when(memberReadPort.findByEmail("user@example.com")).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("user@example.com"))
                .isInstanceOf(UnauthorizedRequestException.class);
    }

    @Test
    @DisplayName("load user quit")
    void loadUserByUsername_quit() {
        Member member = createMember(MemberState.QUIT);
        when(memberReadPort.findByEmail("user@example.com")).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("user@example.com"))
                .isInstanceOf(UnauthorizedRequestException.class);
    }

    @Test
    @DisplayName("load user success")
    void loadUserByUsername_success() {
        Member member = createMember(MemberState.ACTIVE);
        when(memberReadPort.findByEmail("user@example.com")).thenReturn(Optional.of(member));

        CustomUserDetails result = (CustomUserDetails) customUserDetailsService.loadUserByUsername("user@example.com");

        assertThat(result.getEmail()).isEqualTo("user@example.com");
        assertThat(result.getAuthorities()).isNotEmpty();
    }

    private Member createMember(MemberState state) {
        Member member = Member.builder()
                .id(1L)
                .email("user@example.com")
                .memberName("user")
                .memberPwd("pw")
                .tel("01012341234")
                .build();
        ReflectionTestUtils.setField(member, "memberState", state);
        ReflectionTestUtils.setField(member, "role", Role.MEMBER);
        ReflectionTestUtils.setField(member, "lastPasswordChange", LocalDateTime.now());
        return member;
    }
}
