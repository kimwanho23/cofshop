package kwh.cofshop;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Slf4j
@SpringBootTest
public abstract class ServiceTestSetting {

    @Autowired
    protected MemberRepository memberRepository;

    @Autowired
    protected ObjectMapper objectMapper;
}
