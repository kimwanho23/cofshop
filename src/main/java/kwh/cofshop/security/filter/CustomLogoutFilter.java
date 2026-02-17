package kwh.cofshop.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kwh.cofshop.security.cookie.RefreshTokenCookiePolicy;
import kwh.cofshop.security.service.RefreshTokenService;
import kwh.cofshop.security.token.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class CustomLogoutFilter extends OncePerRequestFilter {

    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!request.getRequestURI().equals("/api/auth/logout") || !request.getMethod().equals("POST")) {
            filterChain.doFilter(request, response);
            return;
        }

        String refreshToken = RefreshTokenCookiePolicy.extract(request);

        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)
                || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            response.addHeader(
                    HttpHeaders.SET_COOKIE,
                    RefreshTokenCookiePolicy.expire(isSecureRequest(request)).toString()
            );
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Long memberId = jwtTokenProvider.getMemberId(refreshToken);

        if (!refreshTokenService.matches(memberId, refreshToken)) {
            response.addHeader(
                    HttpHeaders.SET_COOKIE,
                    RefreshTokenCookiePolicy.expire(isSecureRequest(request)).toString()
            );
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        refreshTokenService.delete(memberId);

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                RefreshTokenCookiePolicy.expire(isSecureRequest(request)).toString()
        );
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return forwardedProto != null && "https".equalsIgnoreCase(forwardedProto);
    }

}

