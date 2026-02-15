package kwh.cofshop.config.auditing;

import lombok.NonNull;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class AuditorAwareImpl implements AuditorAware<String> {
    @Override
    @NonNull
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); // 인증된 사용자 정보를 가져온다
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty(); // 인증되지 않은 사용자라면 empty
        }
        return Optional.of(authentication.getName()); // 사용자 이메일 반환
    }

}

