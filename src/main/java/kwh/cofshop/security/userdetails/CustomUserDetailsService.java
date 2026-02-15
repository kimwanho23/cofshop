package kwh.cofshop.security.userdetails;

import kwh.cofshop.global.exception.UnauthorizedRequestException;
import kwh.cofshop.global.exception.errorcodes.UnauthorizedErrorCode;
import kwh.cofshop.member.api.MemberReadPort;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.MemberState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberReadPort memberReadPort;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberReadPort.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Collection<? extends GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + member.getRole())
        );

        if (member.getMemberState() == MemberState.SUSPENDED) {
            throw new UnauthorizedRequestException(UnauthorizedErrorCode.MEMBER_SUSPENDED);
        }

        if (member.getMemberState() == MemberState.QUIT) {
            throw new UnauthorizedRequestException(UnauthorizedErrorCode.MEMBER_QUIT);
        }

        return new CustomUserDetails(
                member.getId(),
                member.getEmail(),
                member.getMemberPwd(),
                authorities,
                member.getMemberState(),
                member.getLastPasswordChange()
        );
    }
}

