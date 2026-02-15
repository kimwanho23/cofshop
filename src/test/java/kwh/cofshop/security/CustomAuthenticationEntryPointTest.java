package kwh.cofshop.security;

import kwh.cofshop.global.exception.UnauthorizedRequestException;
import kwh.cofshop.global.exception.errorcodes.UnauthorizedErrorCode;
import kwh.cofshop.security.handler.CustomAuthenticationEntryPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomAuthenticationEntryPointTest {

    @Mock
    private HandlerExceptionResolver handlerExceptionResolver;

    @Test
    @DisplayName("?몄쬆 ?ㅽ뙣???꾩뿭 ?덉쇅 泥섎━濡??꾩엫?쒕떎")
    void commence_delegatesToHandlerExceptionResolver() throws Exception {
        CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint(handlerExceptionResolver);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                request,
                response,
                new InsufficientAuthenticationException("Authentication required")
        );

        ArgumentCaptor<Exception> exceptionCaptor = ArgumentCaptor.forClass(Exception.class);
        verify(handlerExceptionResolver).resolveException(eq(request), eq(response), isNull(), exceptionCaptor.capture());

        Exception resolved = exceptionCaptor.getValue();
        assertThat(resolved).isInstanceOf(UnauthorizedRequestException.class);
        UnauthorizedRequestException unauthorizedException = (UnauthorizedRequestException) resolved;
        assertThat(unauthorizedException.getErrorCode()).isEqualTo(UnauthorizedErrorCode.TOKEN_INVALID);
    }
}
