package kwh.cofshop.security;

import kwh.cofshop.global.exception.ForbiddenRequestException;
import kwh.cofshop.global.exception.errorcodes.ForbiddenErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomAccessDeniedHandlerTest {

    @Mock
    private HandlerExceptionResolver handlerExceptionResolver;

    @Test
    @DisplayName("인가 실패는 전역 예외 처리로 위임된다")
    void handle_delegatesToHandlerExceptionResolver() throws Exception {
        CustomAccessDeniedHandler deniedHandler = new CustomAccessDeniedHandler(handlerExceptionResolver);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        deniedHandler.handle(request, response, new AccessDeniedException("Access denied"));

        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(handlerExceptionResolver).resolveException(eq(request), eq(response), isNull(), exceptionCaptor.capture());

        Exception resolved = exceptionCaptor.getValue();
        assertThat(resolved).isInstanceOf(ForbiddenRequestException.class);
        ForbiddenRequestException forbiddenException = (ForbiddenRequestException) resolved;
        assertThat(forbiddenException.getErrorCode()).isEqualTo(ForbiddenErrorCode.ACCESS_DENIED);
    }
}
