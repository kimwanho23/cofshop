package kwh.cofshop.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kwh.cofshop.security.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class CustomLogoutFilter extends OncePerRequestFilter {


    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. 로그아웃 요청 확인
        if (!request.getRequestURI().equals("/logout") || !request.getMethod().equals("POST")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 쿠키에서 refreshToken 추출
        String refreshToken = extractRefreshToken(request);

        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // 3. JWT에서 memberId 추출
        Long memberId = jwtTokenProvider.getMemberId(refreshToken);

        // 4. Redis에 저장된 토큰과 일치하는지 확인
        if (!refreshTokenService.matches(memberId, refreshToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 5. Redis에서 RefreshToken 삭제
        refreshTokenService.delete(memberId);

        // 쿠키 삭제
        Cookie deleteCookie = new Cookie("refreshToken", null);
        deleteCookie.setMaxAge(0);
        deleteCookie.setPath("/");
        response.addCookie(deleteCookie);
        response.setStatus(HttpServletResponse.SC_OK);
    }


    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null)
            return null;

        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}

