package kwh.cofshop.security.provider;

import kwh.cofshop.security.userdetails.CustomUserDetails;
import kwh.cofshop.security.userdetails.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@RequiredArgsConstructor
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String password = authentication.getCredentials().toString();

        CustomUserDetails customUserDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(email);

        if (!(customUserDetails.getUsername().equals(email)
                && bCryptPasswordEncoder.matches(password, customUserDetails.getPassword()))) {
            throw new BadCredentialsException("Invalid credentials");
        }

        // 비밀번호 확인 후 검사해 다른 사람이 계정 상태를 알아낼 수 없게 함
        if (!customUserDetails.isAccountNonLocked()) {
            throw new LockedException("Suspended member");
        }
        if (!customUserDetails.isAccountNonExpired()) {
            throw new DisabledException("Quit member");
        }
        return new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

