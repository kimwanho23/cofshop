package kwh.cofshop;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;


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

    protected String getToken() {
        Member member = memberRepository.findByEmail("test@gmail.com").orElseThrow();
        Authentication authentication = new UsernamePasswordAuthenticationToken(member.getEmail(), null);
        return jwtTokenProvider.createAuthToken(authentication).getAccessToken();
    }
}
