package kwh.cofshop.argumentresolver;

import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.UnauthorizedRequestException;
import kwh.cofshop.global.exception.errorcodes.UnauthorizedErrorCode;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.security.token.JwtTokenProvider;
import kwh.cofshop.security.userdetails.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginMemberArgumentResolverTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증 정보의 회원 ID를 Long 파라미터로 전달")
    void resolve_memberId() {
        authenticate(userDetails());
        assertThat(resolve(Long.class, null)).isEqualTo(7L);

        authenticate(9L);
        assertThat(resolve(long.class, null)).isEqualTo(9L);
    }

    @Test
    @DisplayName("CustomUserDetails 파라미터는 인증 주체를 그대로 전달")
    void resolve_userDetails() {
        CustomUserDetails userDetails = userDetails();
        authenticate(userDetails);

        assertThat(resolve(CustomUserDetails.class, null)).isSameAs(userDetails);
    }

    @Test
    @DisplayName("주체 타입이 파라미터와 맞지 않으면 인증 실패")
    void resolve_wrongPrincipal() {
        authenticate("someone");

        assertUnauthorized(Long.class, null, UnauthorizedErrorCode.TOKEN_INVALID);
        assertUnauthorized(CustomUserDetails.class, null, UnauthorizedErrorCode.TOKEN_INVALID);
    }

    @Test
    @DisplayName("지원하지 않는 파라미터 타입은 잘못된 요청")
    void resolve_unsupportedType() {
        authenticate(9L);

        assertThatThrownBy(() -> resolve(String.class, null)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("인증 정보가 없으면 토큰 상태에 맞는 오류로 응답")
    void resolve_unauthenticated() {
        assertUnauthorized(Long.class, null, UnauthorizedErrorCode.TOKEN_NOT_EXIST);
        assertUnauthorized(Long.class, "Basic abc", UnauthorizedErrorCode.TOKEN_NOT_EXIST);

        when(jwtTokenProvider.getTokenStatus("expired")).thenReturn(JwtTokenProvider.TokenStatus.EXPIRED);
        assertUnauthorized(Long.class, "Bearer expired", UnauthorizedErrorCode.TOKEN_EXPIRED);

        when(jwtTokenProvider.getTokenStatus("forged")).thenReturn(JwtTokenProvider.TokenStatus.INVALID);
        assertUnauthorized(Long.class, "Bearer forged", UnauthorizedErrorCode.TOKEN_INVALID);
    }

    private void assertUnauthorized(Class<?> type, String authorization, UnauthorizedErrorCode expected) {
        assertThatThrownBy(() -> resolve(type, authorization))
                .isInstanceOf(UnauthorizedRequestException.class)
                .extracting("errorCode")
                .isEqualTo(expected);
    }

    private Object resolve(Class<?> type, String authorization) {
        MethodParameter parameter = mock(MethodParameter.class);
        org.mockito.Mockito.lenient().doReturn(type).when(parameter).getParameterType();
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        NativeWebRequest webRequest = new ServletWebRequest(request);
        return new LoginMemberArgumentResolver(jwtTokenProvider).resolveArgument(parameter, null, webRequest, null);
    }

    private void authenticate(Object principal) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(principal, null, "ROLE_MEMBER");
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private CustomUserDetails userDetails() {
        return new CustomUserDetails(7L, "user@example.com", "pw", List.of(), MemberState.ACTIVE, LocalDateTime.now());
    }
}
