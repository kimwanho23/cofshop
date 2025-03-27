package kwh.cofshop.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kwh.cofshop.global.response.ApiResponse;
import kwh.cofshop.security.domain.RefreshToken;
import kwh.cofshop.security.dto.TokenDto;
import kwh.cofshop.member.dto.request.LoginDto;
import kwh.cofshop.member.dto.response.LoginResponseDto;
import kwh.cofshop.security.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

// Member ID, PASSWORD 전달받아서, 유효 사용자 검증 후 인증 Filter
@Slf4j
@RequiredArgsConstructor
public class CustomLoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            LoginDto loginDto = objectMapper.readValue(request.getInputStream(), LoginDto.class);
            UsernamePasswordAuthenticationToken authRequest =
                    new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getMemberPwd());

            setDetails(request, authRequest);
            return authenticationManager.authenticate(authRequest);
        } catch (IOException e) {
            throw new AuthenticationServiceException("로그인 요청 본문을 읽을 수 없습니다.", e);
        }
    }

    // attemptAuthentication 인증이 정상적으로 이루어졌다면 해당 메소드 실행된다.
    // 즉, JWT 토큰을 만들어서 request 요청한 사용자에게 reponse한다.
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult) throws IOException, ServletException {
        CustomUserDetails customUserDetails = (CustomUserDetails) authResult.getPrincipal();
        TokenDto token = jwtTokenProvider.createAuthToken(authResult);

        addRefreshToken(customUserDetails.getId(), token.getRefreshToken()); // 리프레시 토큰을 저장한다.

        response.setHeader("Authorization", "Bearer " + token.getAccessToken()); // Access 토큰을 헤더에 저장
        response.addCookie(createCookie("refreshToken", token.getRefreshToken())); // Refresh 토큰을 쿠키로 넘긴다.
        response.setStatus(HttpServletResponse.SC_OK);

        LoginResponseDto tokenResponse = LoginResponseDto.builder()
                .accessToken(token.getAccessToken())
                .refreshToken(token.getRefreshToken())
                .memberId(customUserDetails.getId())
                .email(customUserDetails.getEmail())
                .passwordChangeRequired(customUserDetails.isCredentialsNonExpired())
                .build();

        ApiResponse<LoginResponseDto> apiResponse = ApiResponse.OK(tokenResponse);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
    private void addRefreshToken(Long memberId, String token) {
        LocalDateTime expiration = LocalDateTime.now().plusDays(7);

        Optional<RefreshToken> optionalToken = refreshTokenRepository.findByMemberId(memberId);

        if (optionalToken.isPresent()) {
            RefreshToken existingToken = optionalToken.get();
            existingToken.update(token, expiration);
        } else {
            // 존재하지 않으면 새로 생성해서 저장
            RefreshToken newToken = RefreshToken.builder()
                    .memberId(memberId)
                    .refresh(token)
                    .expiration(expiration)
                    .build();
            refreshTokenRepository.save(newToken);
        }
    }

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24*60*60);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }
}