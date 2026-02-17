package kwh.cofshop.security.filter;

import jakarta.servlet.http.Cookie;
import kwh.cofshop.security.service.RefreshTokenService;
import kwh.cofshop.security.token.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomLogoutFilterTest {

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private CustomLogoutFilter customLogoutFilter;

    @BeforeEach
    void setUp() {
        customLogoutFilter = new CustomLogoutFilter(refreshTokenService, jwtTokenProvider);
    }

    @Test
    @DisplayName("로그아웃 요청의 refresh token이 유효하지 않으면 쿠키를 만료시키고 400을 반환한다")
    void logout_invalidToken() throws Exception {
        MockHttpServletRequest request = logoutRequest("invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

        customLogoutFilter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(400);
        assertExpiredRefreshCookie(response);
    }

    @Test
    @DisplayName("저장된 refresh token과 불일치하면 쿠키를 만료시키고 401을 반환한다")
    void logout_tokenMismatch() throws Exception {
        MockHttpServletRequest request = logoutRequest("valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getMemberId("valid-token")).thenReturn(1L);
        when(refreshTokenService.matches(1L, "valid-token")).thenReturn(false);

        customLogoutFilter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertExpiredRefreshCookie(response);
    }

    @Test
    @DisplayName("로그아웃 성공 시 refresh token을 삭제하고 쿠키를 만료시킨다")
    void logout_success() throws Exception {
        MockHttpServletRequest request = logoutRequest("valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.isRefreshToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getMemberId("valid-token")).thenReturn(1L);
        when(refreshTokenService.matches(1L, "valid-token")).thenReturn(true);

        customLogoutFilter.doFilter(request, response, new MockFilterChain());

        verify(refreshTokenService).delete(1L);
        assertThat(response.getStatus()).isEqualTo(200);
        assertExpiredRefreshCookie(response);
    }

    private static MockHttpServletRequest logoutRequest(String refreshToken) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/logout");
        request.setMethod("POST");
        request.setCookies(new Cookie("refreshToken", refreshToken));
        return request;
    }

    private static void assertExpiredRefreshCookie(MockHttpServletResponse response) {
        assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
                .contains("refreshToken=")
                .contains("Path=/api/auth")
                .contains("Max-Age=0");
    }
}
