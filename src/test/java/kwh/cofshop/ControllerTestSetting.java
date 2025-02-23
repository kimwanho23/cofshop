package kwh.cofshop;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.security.CustomUserDetails;
import kwh.cofshop.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collection;
import java.util.List;


@SpringBootTest
@AutoConfigureMockMvc
public abstract class ControllerTestSetting {

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected MemberRepository memberRepository;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    protected Authentication createTestAuthentication(Member member) {
        Collection<? extends GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + member.getRole())
        );

        CustomUserDetails userDetails = new CustomUserDetails(
                member.getId(),
                member.getEmail(),
                "",
                authorities,
                member.getMemberState(),
                member.getLastPasswordChange()
        );
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    protected String getToken() {
        Member member = memberRepository.findByEmail("test@gmail.com").orElseThrow();
        Authentication testAuthentication = createTestAuthentication(member);
        return jwtTokenProvider.createAuthToken(testAuthentication).getAccessToken();
    }


}