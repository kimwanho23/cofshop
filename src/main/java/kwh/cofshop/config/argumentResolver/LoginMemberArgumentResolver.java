package kwh.cofshop.config.argumentResolver;

import jakarta.servlet.http.HttpSession;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Objects;

@RequiredArgsConstructor
@Component
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean isLoginUserAnnotation = parameter.getParameterAnnotation(LoginMember.class) != null;
        boolean isUserClass = Member.class.
                equals(parameter.getParameterType());
        return isLoginUserAnnotation && isUserClass;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        String token = Objects.requireNonNull(webRequest.getHeader("Authorization")).substring(7); // "Bearer " 제거
        String email = jwtTokenProvider.getClaims(token).getSubject(); // JWT에서 이메일 추출
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
    }
}

