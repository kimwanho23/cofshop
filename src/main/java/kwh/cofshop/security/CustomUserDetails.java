
package kwh.cofshop.security;

import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.member.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

@Getter
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {
    private Member member;

    private GrantedAuthority getAuthority(Role role) {
        return new SimpleGrantedAuthority("ROLE_" + role);
    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        ArrayList<GrantedAuthority> auth = new ArrayList<>();
        switch (member.getRole()){
            case ADMIN :
                auth.add(getAuthority(Role.ADMIN));
            case MEMBER :
                auth.add(getAuthority(Role.MEMBER));
        }
        return auth;
    }
    @Override
    public String getUsername() { // 아이디 (이메일로 로그인)
        return member.getEmail();
    }

    @Override
    public String getPassword() { // 비밀번호
        return member.getMemberPwd();
    }

    @Override
    public boolean isAccountNonExpired() { // 멤버 계정 만료 여부
        return member.getMemberState() != MemberState.QUIT; // QUIT 일때는 비활성화 상태
    }

    @Override
    public boolean isAccountNonLocked() { // 계정 잠금 상태
        return member.getMemberState() != MemberState.SUSPENDED; // 정지 당했을 때
    }

    @Override
    public boolean isCredentialsNonExpired() { // 비밀번호 만료 여부
        return member.getLastPasswordChange().isAfter(LocalDateTime.now().minusDays(30)); // 30일 주기로 설정
    }

    @Override
    public boolean isEnabled() { // 사용자 활성화 여부
        return member.getMemberState() == MemberState.ACTIVE;
    }

}

