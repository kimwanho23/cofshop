package kwh.cofshop.argumentResolver;

import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.annotation.LoginMember;
import kwh.cofshop.global.exception.UnauthorizedRequestException;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import kwh.cofshop.global.exception.errorcodes.UnauthorizedErrorCode;
import kwh.cofshop.security.userdetails.CustomUserDetails;
import kwh.cofshop.security.token.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@RequiredArgsConstructor
@Component
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginMember.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw resolveUnauthorized(webRequest);
        }

        Object principal = authentication.getPrincipal();
        Class<?> parameterType = parameter.getParameterType();

        if (CustomUserDetails.class.isAssignableFrom(parameterType)) {
            if (!(principal instanceof CustomUserDetails)) {
                throw new UnauthorizedRequestException(UnauthorizedErrorCode.TOKEN_INVALID);
            }
            return principal;
        }

        if (Long.class.equals(parameterType) || long.class.equals(parameterType)) {
            if (principal instanceof CustomUserDetails userDetails) {
                return userDetails.getId();
            }
            if (principal instanceof Long memberId) {
                return memberId;
            }
            throw new UnauthorizedRequestException(UnauthorizedErrorCode.TOKEN_INVALID);
        }

        throw new BadRequestException(BadRequestErrorCode.BAD_REQUEST);
    }

    private UnauthorizedRequestException resolveUnauthorized(NativeWebRequest webRequest) {
        String token = resolveToken(webRequest);
        if (token == null) {
            return new UnauthorizedRequestException(UnauthorizedErrorCode.TOKEN_NOT_EXIST);
        }

        JwtTokenProvider.TokenStatus status = jwtTokenProvider.getTokenStatus(token);
        if (status == JwtTokenProvider.TokenStatus.EXPIRED) {
            return new UnauthorizedRequestException(UnauthorizedErrorCode.TOKEN_EXPIRED);
        }
        if (status != JwtTokenProvider.TokenStatus.INVALID) {
            jwtTokenProvider.isAccessToken(token);
        }

        return new UnauthorizedRequestException(UnauthorizedErrorCode.TOKEN_INVALID);
    }

    private String resolveToken(NativeWebRequest webRequest) {
        jakarta.servlet.http.HttpServletRequest request = webRequest.getNativeRequest(jakarta.servlet.http.HttpServletRequest.class);
        if (request == null) {
            return null;
        }
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}

