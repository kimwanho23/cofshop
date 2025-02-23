package kwh.cofshop.member.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.Role;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.member.dto.LoginDto;
import kwh.cofshop.member.dto.LoginResponseDto;
import kwh.cofshop.member.dto.MemberRequestDto;
import kwh.cofshop.member.dto.MemberResponseDto;
import kwh.cofshop.member.mapper.MemberMapper;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
class MemberServiceTest {

    @Autowired
    MemberService memberService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    private ObjectMapper objectMapper;  // JSON 변환 도구

    @Autowired
    private MemberMapper memberMapper;  // JSON 변환 도구

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("회원가입 로직 테스트")
    @Transactional
   // @Commit
    void signUp() throws JsonProcessingException {  // 1번 테스트

        //DTO 생성 (프론트에서 입력)
        MemberRequestDto requestDto = new MemberRequestDto();
        requestDto.setEmail("test@gmail.com");
        requestDto.setMemberName("테스트");
        requestDto.setMemberPwd("1234567890");
        requestDto.setTel("010-1234-5678");

        // 저장
        MemberResponseDto savedMember = memberService.save(requestDto);

        log.info(objectMapper.writeValueAsString(savedMember));

        // 데이터 검증
        assertThat(savedMember).isNotNull();
        assertThat(savedMember.getEmail()).isEqualTo("test@gmail.com");
        assertThat(savedMember.getMemberName()).isEqualTo("테스트");
        assertThat(savedMember.getTel()).isEqualTo("010-1234-5678");

        // 기본값 검증
        assertThat(savedMember.getPoint()).isEqualTo(0);  // 기본 포인트
        assertThat(savedMember.getMemberState()).isEqualTo(MemberState.ACTIVE);  // 기본 상태
        assertThat(savedMember.getRole()).isEqualTo(Role.MEMBER);  // 기본 권한
        assertThat(savedMember.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());  // 가입일자
        assertThat(savedMember.getLastPasswordChange()).isBeforeOrEqualTo(LocalDateTime.now());  // 비밀번호 변경일자
        assertThat(savedMember.getLastLogin()).isNull();  // 기본값 NULL(로그인 하지 않았다면 NULL이 된다. 로그인 로직 시 lastLogin을 변환해야 한다.)
        // 즉, 로그인 로직 테스트 시에 lastLogin이 LocalDateTime.now()가 맞는 지 검증해야 한다.

        // 테스트 통과 (2024-12-11)
    }

    @Test
    @DisplayName("멤버 Find, ResponseDto 테스트")
    @Transactional
    void findMember(){  // 2번 테스트
        MemberResponseDto findMember = memberService.findMember(2L);

        assertThat(findMember.getEmail()).isEqualTo("test@gmail.com");
        assertThat(findMember.getMemberName()).isEqualTo("테스트");
        assertThat(findMember.getTel()).isEqualTo("010-1234-5678");

        // 테스트 통과 (2024-12-11)
    }

    @Test
    @DisplayName("로그인")
    void login(){
        LoginDto loginDto = new LoginDto();
        loginDto.setEmail("test@gmail.com");
        loginDto.setMemberPwd("1234567890");
        LoginResponseDto login = memberService.login(loginDto);
        log.info(login.getAccessToken());
        log.info(login.getRefreshToken());
        log.info(String.valueOf(login.isPasswordChangeRequired()));

    }

    @Test
    @DisplayName("멤버 상태 변경")
    @Transactional
    @Commit
    void changeMemberState() throws Exception {
        Member member = memberRepository.findByEmail("test@gmail.com").orElseThrow();
        member.changeMemberState(MemberState.ACTIVE);
        MemberResponseDto responseDto = memberMapper.toResponseDto(member);
        log.info(objectMapper.writeValueAsString(responseDto));
    }



}