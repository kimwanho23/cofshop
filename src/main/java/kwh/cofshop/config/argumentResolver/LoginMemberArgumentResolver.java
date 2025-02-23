package kwh.cofshop.config.argumentResolver;

import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ClassUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.annotation.Annotation;
import java.util.Objects;

@RequiredArgsConstructor
@Component
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginMember.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("인증되지 않은 사용자입니다."); // 로그인 하지 않은 사용자. getPrincipal은 로그인이 되어있지 않으면 annonymousUser를 반환
        }

        Object principal = authentication.getPrincipal(); // 사용자 ( 주체 ),

        // 타입 검증, CustomUserDetails
        if (!ClassUtils.isAssignable(principal.getClass(), parameter.getParameterType())) {
            throw new ClassCastException("Principal 객체가 " + parameter.getParameterType() + "로 변환될 수 없습니다.");
        }

        return principal;
    }
}

