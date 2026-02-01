package kwh.cofshop.support;

import kwh.cofshop.argumentResolver.LoginMember;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.security.CustomUserDetails;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

public class TestLoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (!parameter.hasParameterAnnotation(LoginMember.class)) {
            return false;
        }
        Class<?> parameterType = parameter.getParameterType();
        return CustomUserDetails.class.isAssignableFrom(parameterType)
                || Long.class.equals(parameterType)
                || long.class.equals(parameterType);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        Class<?> parameterType = parameter.getParameterType();
        if (Long.class.equals(parameterType) || long.class.equals(parameterType)) {
            return 1L;
        }
        return new CustomUserDetails(
                1L,
                "test@example.com",
                "",
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER")),
                MemberState.ACTIVE,
                LocalDateTime.now()
        );
    }
}
