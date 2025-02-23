
package kwh.cofshop.security;

import kwh.cofshop.global.exception.UnauthorizedRequestException;
import kwh.cofshop.global.exception.errorcodes.UnauthorizedErrorCode;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(String.format("%s은(는) 없는 이메일 입니다. 다시 확인해주세요.", email)));

        Collection<? extends GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + member.getRole())
        );

        // 상태별로 인증 실패 처리
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

