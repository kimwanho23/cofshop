package kwh.cofshop.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kwh.cofshop.global.exception.UnauthorizedRequestException;
import kwh.cofshop.global.exception.errorcodes.UnauthorizedErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) {
        handlerExceptionResolver.resolveException(
                request,
                response,
                null,
                new UnauthorizedRequestException(toErrorCode(exception))
        );
    }

    // 계정 상태는 비밀번호가 맞을 때만 알려줌. 그 외 실패는 원인을 구분하지 않음
    private UnauthorizedErrorCode toErrorCode(AuthenticationException exception) {
        if (exception instanceof LockedException) {
            return UnauthorizedErrorCode.MEMBER_SUSPENDED;
        }
        if (exception instanceof DisabledException) {
            return UnauthorizedErrorCode.MEMBER_QUIT;
        }
        return UnauthorizedErrorCode.LOGIN_FAILED;
    }
}
