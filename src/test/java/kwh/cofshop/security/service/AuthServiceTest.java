package kwh.cofshop.security.service;

import jakarta.servlet.http.Cookie;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.UnauthorizedRequestException;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.Role;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.security.JwtTokenProvider;
import kwh.cofshop.security.dto.TokenDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Reissue fails when cookie is missing")
    void reissue_noCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> authService.reissue(request, response))
                .isInstanceOf(UnauthorizedRequestException.class);
    }

    @Test
    @DisplayName("Reissue fails when token is invalid")
    void reissue_invalidToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setCookies(new Cookie("refreshToken", "token"));

        when(jwtTokenProvider.validateToken("token")).thenReturn(false);

        assertThatThrownBy(() -> authService.reissue(request, response))
                .isInstanceOf(UnauthorizedRequestException.class);
    }

    @Test
    @DisplayName("Reissue fails when token does not match stored token")
    void reissue_tokenMismatch() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setCookies(new Cookie("refreshToken", "token"));

        when(jwtTokenProvider.validateToken("token")).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken("token")).thenReturn(true);
        when(jwtTokenProvider.getMemberId("token")).thenReturn(1L);
        when(refreshTokenService.matches(1L, "token")).thenReturn(false);

        assertThatThrownBy(() -> authService.reissue(request, response))
                .isInstanceOf(UnauthorizedRequestException.class);
    }

    @Test
    @DisplayName("Reissue fails when member is not found")
    void reissue_memberNotFound() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setCookies(new Cookie("refreshToken", "token"));

        when(jwtTokenProvider.validateToken("token")).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken("token")).thenReturn(true);
        when(jwtTokenProvider.getMemberId("token")).thenReturn(1L);
        when(refreshTokenService.matches(1L, "token")).thenReturn(true);
        when(memberRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.reissue(request, response))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Reissue returns new access and refresh tokens")
    void reissue_success() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setCookies(new Cookie("refreshToken", "token"));

        Member member = Member.builder()
                .id(1L)
                .email("user@example.com")
                .memberName("user")
                .memberPwd("pw")
                .tel("01012341234")
                .build();
        ReflectionTestUtils.setField(member, "role", Role.MEMBER);

        when(jwtTokenProvider.validateToken("token")).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken("token")).thenReturn(true);
        when(jwtTokenProvider.getMemberId("token")).thenReturn(1L);
        when(refreshTokenService.matches(1L, "token")).thenReturn(true);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(jwtTokenProvider.createAccessToken(anyLong(), any(), any())).thenReturn("access");
        when(jwtTokenProvider.createRefreshToken(1L, "user@example.com")).thenReturn("refresh");

        TokenDto responseDto = authService.reissue(request, response);

        assertThat(responseDto.getAccessToken()).isEqualTo("access");
        assertThat(responseDto.getRefreshToken()).isNull();
        assertThat(response.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer access");
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
                .contains("refreshToken=refresh")
                .contains("SameSite=Strict");
        verify(refreshTokenService).save(1L, "refresh");
    }
}
