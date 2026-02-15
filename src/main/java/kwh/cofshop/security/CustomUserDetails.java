package kwh.cofshop.security;

import kwh.cofshop.member.domain.MemberState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;

@Getter
@AllArgsConstructor
@Builder
@Slf4j
public class CustomUserDetails implements UserDetails {
    private Long id;
    private String email;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    private MemberState memberState;
    private LocalDateTime lastPasswordChange;

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() { // 계정 비활성화 여부
        return memberState != MemberState.QUIT;
    }

    @Override
    public boolean isAccountNonLocked() { // 계정 정지 여부
        return memberState != MemberState.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() { // 비밀번호 변경시간
        return lastPasswordChange.isAfter(LocalDateTime.now().minusDays(90)); // 90일 이내일 시는 true
    }

    @Override
    public boolean isEnabled() { // 계정 활성화 여부
        return memberState == MemberState.ACTIVE;
    }

}