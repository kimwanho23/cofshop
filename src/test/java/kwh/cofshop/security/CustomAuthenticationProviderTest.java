package kwh.cofshop.security;

import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.security.provider.CustomAuthenticationProvider;
import kwh.cofshop.security.userdetails.CustomUserDetails;
import kwh.cofshop.security.userdetails.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomAuthenticationProviderTest {

    private static final String EMAIL = "user@example.com";
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("정상 회원: 비밀번호가 맞으면 인증")
    void authenticate_active() {
        givenMember(MemberState.ACTIVE);

        assertThat(authenticate("correct").isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("정지·탈퇴 회원: 비밀번호가 틀리면 상태와 무관하게 같은 실패")
    void authenticate_wrongPassword_hidesState() {
        givenMember(MemberState.SUSPENDED);
        assertThatThrownBy(() -> authenticate("wrong")).isInstanceOf(BadCredentialsException.class);

        givenMember(MemberState.QUIT);
        assertThatThrownBy(() -> authenticate("wrong")).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("정지 회원: 비밀번호가 맞으면 정지 실패")
    void authenticate_suspended() {
        givenMember(MemberState.SUSPENDED);

        assertThatThrownBy(() -> authenticate("correct")).isInstanceOf(LockedException.class);
    }

    @Test
    @DisplayName("탈퇴 회원: 비밀번호가 맞으면 탈퇴 실패")
    void authenticate_quit() {
        givenMember(MemberState.QUIT);

        assertThatThrownBy(() -> authenticate("correct")).isInstanceOf(DisabledException.class);
    }

    private org.springframework.security.core.Authentication authenticate(String password) {
        return new CustomAuthenticationProvider(ENCODER, userDetailsService)
                .authenticate(new UsernamePasswordAuthenticationToken(EMAIL, password));
    }

    private void givenMember(MemberState state) {
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(new CustomUserDetails(
                1L, EMAIL, ENCODER.encode("correct"), List.of(), state, LocalDateTime.now()));
    }
}
