package kwh.cofshop.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kwh.cofshop.security.dto.request.LoginRequestDto;
import kwh.cofshop.security.dto.response.LoginResponseDto;
import kwh.cofshop.member.event.MemberLoginEvent;
import kwh.cofshop.security.cookie.RefreshTokenCookiePolicy;
import kwh.cofshop.security.dto.TokenResponseDto;
import kwh.cofshop.security.service.RefreshTokenService;
import kwh.cofshop.security.token.JwtTokenProvider;
import kwh.cofshop.security.userdetails.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

// Member ID, PASSWORD -> user validation
@Slf4j
@RequiredArgsConstructor
public class CustomLoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final RefreshTokenService refreshTokenService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Validator validator;

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        try {
            LoginRequestDto loginDto = objectMapper.readValue(request.getInputStream(), LoginRequestDto.class);
            validateLoginPayload(loginDto);
            UsernamePasswordAuthenticationToken authRequest =
                    new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getMemberPwd());

            setDetails(request, authRequest);
            return authenticationManager.authenticate(authRequest);
        } catch (IOException e) {
            throw new AuthenticationServiceException("Invalid login payload.", e);
        }
    }

    private void validateLoginPayload(LoginRequestDto loginDto) {
        if (loginDto == null) {
            throw new AuthenticationServiceException("Invalid login payload.");
        }
        Set<ConstraintViolation<LoginRequestDto>> violations = validator.validate(loginDto);
        if (violations.isEmpty()) {
            return;
        }

        String message = violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        throw new AuthenticationServiceException(message);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult)
            throws IOException, ServletException {
        CustomUserDetails customUserDetails = (CustomUserDetails) authResult.getPrincipal();
        TokenResponseDto token = jwtTokenProvider.createAuthToken(authResult);

        addRefreshToken(customUserDetails.getId(), token.getRefreshToken());

        response.setHeader("Authorization", "Bearer " + token.getAccessToken());
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                RefreshTokenCookiePolicy.issue(token.getRefreshToken(), isSecureRequest(request)).toString()
        );
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

    private boolean isSecureRequest(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return forwardedProto != null && "https".equalsIgnoreCase(forwardedProto);
    }
}

