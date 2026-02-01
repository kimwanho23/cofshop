package kwh.cofshop.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kwh.cofshop.member.dto.request.LoginDto;
import kwh.cofshop.member.dto.response.LoginResponseDto;
import kwh.cofshop.member.event.MemberLoginEvent;
import kwh.cofshop.security.dto.TokenDto;
import kwh.cofshop.security.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.time.LocalDateTime;

// Member ID, PASSWORD -> user validation
@Slf4j
@RequiredArgsConstructor
public class CustomLoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final RefreshTokenService refreshTokenService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        try {
            LoginDto loginDto = objectMapper.readValue(request.getInputStream(), LoginDto.class);
            UsernamePasswordAuthenticationToken authRequest =
                    new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getMemberPwd());

            setDetails(request, authRequest);
            return authenticationManager.authenticate(authRequest);
        } catch (IOException e) {
            throw new AuthenticationServiceException("Invalid login payload.", e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult)
            throws IOException, ServletException {
        CustomUserDetails customUserDetails = (CustomUserDetails) authResult.getPrincipal();
        TokenDto token = jwtTokenProvider.createAuthToken(authResult);

        addRefreshToken(customUserDetails.getId(), token.getRefreshToken());

        response.setHeader("Authorization", "Bearer " + token.getAccessToken());
        response.addHeader(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(token.getRefreshToken()).toString());
        response.setStatus(HttpServletResponse.SC_OK);

        MemberLoginEvent memberLoginHistoryDto = MemberLoginEvent
                .builder()
                .loginDt(LocalDateTime.now())
                .device(request.getHeader("User-Agent"))
                .ipAddress(request.getRemoteAddr())
                .memberId(customUserDetails.getId())
                .build();

        applicationEventPublisher.publishEvent(memberLoginHistoryDto);

        LoginResponseDto tokenResponse = LoginResponseDto.builder()
                .accessToken(token.getAccessToken())
                .memberId(customUserDetails.getId())
                .email(customUserDetails.getEmail())
                .passwordChangeRequired(!customUserDetails.isCredentialsNonExpired())
                .build();

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(tokenResponse));
    }

    private void addRefreshToken(Long memberId, String token) {
        refreshTokenService.save(memberId, token);
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(7 * 24 * 60 * 60L)
                .build();
    }
}
