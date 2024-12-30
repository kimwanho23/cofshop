package kwh.cofshop.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kwh.cofshop.global.TokenDto;
import kwh.cofshop.member.dto.LoginDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Map;

// Member ID, PASSWORD 전달받아서, 유효 사용자 검증 후 인증 Filter
@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginFilter(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // Json(Email, Password) -> Dto 변환
            LoginDto loginDto = objectMapper.readValue(request.getInputStream(), LoginDto.class);

            // UsernamePasswordAuthenticationToken 생성
            UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(
                    loginDto.getEmail(), loginDto.getMemberPwd());

            // 사용자 요청 세부 정보 설정

            setDetails(request, authRequest);

            return authenticationManager.authenticate(authRequest);
        } catch (IOException e) {
            throw new AuthenticationServiceException("로그인 요청 본문을 읽을 수 없습니다.", e);
        }
    }

    // attemptAuthentication 인증이 정상적으로 이루어졌다면 해당 메소드 실행된다.
    // 즉, JWT 토큰을 만들어서 request 요청한 사용자에게 reponse한다.
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        ObjectMapper objectMapper = new ObjectMapper();
        CustomUserDetails customUserDetails = (CustomUserDetails) authResult.getPrincipal(); // 로그인 성공 유저
        log.info("인증 성공: {}", authResult.getName());

        // AccessToken 및 RefreshToken 생성
        TokenDto token = jwtTokenProvider.createAuthToken(customUserDetails);

        log.info("헤더 설정");
        response.addHeader("Authorization", "Bearer " + token.getAccessToken());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Map<String, String> tokenResponse = Map.of(
                "accessToken", token.getAccessToken(),
                "refreshToken", token.getRefreshToken(),
                "email", customUserDetails.getUsername()
        );
        response.getWriter().write(objectMapper.writeValueAsString(tokenResponse));
    }
}