package kwh.cofshop.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 인가 처리 필터
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    public JwtFilter(JwtTokenProvider jwtTokenProvider, MemberRepository memberRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.memberRepository = memberRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String jwtHeader = request.getHeader("Authorization");
        log.info(jwtHeader);

        String path = request.getRequestURI();

        // Swagger 경로 제외
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs") || path.startsWith("/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (jwtHeader == null || !jwtHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            log.info("토큰이 없습니다.");
            return;
        }
        String token = jwtHeader.replace("Bearer ", "");
        log.info("오긴하냐");


        if (jwtTokenProvider.isTokenValid(token)) { // 토큰 유효성 검사
            Claims claims = jwtTokenProvider.getClaims(token);
            String email = claims.getSubject();


            memberRepository.findByEmail(email).ifPresent(member -> {
                CustomUserDetails customUserDetails = new CustomUserDetails(member);
                // JWT 토큰 서명을 통해서 서명이 정상이면 Authentication 객체를 만들어 준다.
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        customUserDetails, null, customUserDetails.getAuthorities());
                // 강제로 Security 세션에 접근하여 Authentication 객체를 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });

            filterChain.doFilter(request, response);
        } else { // 토큰 만료 시 접근 불가
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // 401 응답 설정
            response.getWriter().write("{\"message\": \"JWT 토큰이 만료되었습니다.\"}");
            response.setContentType("application/json;charset=UTF-8");
        }
    }
}
