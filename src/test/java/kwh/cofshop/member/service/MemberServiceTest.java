package kwh.cofshop.member.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.TestSettingUtils;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.Role;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.member.dto.request.LoginDto;
import kwh.cofshop.member.dto.request.MemberRequestDto;
import kwh.cofshop.member.dto.response.LoginResponseDto;
import kwh.cofshop.member.dto.response.MemberResponseDto;
import kwh.cofshop.member.mapper.MemberMapper;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.security.JwtTokenProvider;
import kwh.cofshop.security.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureMockMvc
@Slf4j
class MemberServiceTest extends TestSettingUtils {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ObjectMapper objectMapper;  // JSON 변환 도구

    @Autowired
    private JwtTokenProvider jwtTokenProvider;


    @Test
    @DisplayName("회원가입 테스트")
    @Transactional
    void signUp() throws JsonProcessingException {  // 1번 테스트

        //DTO 생성 (프론트에서 입력)
        MemberRequestDto requestDto = getMemberRequestDto("test2@gmail.com");

        // 저장
        MemberResponseDto savedMember = memberService.signUp(requestDto);

        log.info(objectMapper.writeValueAsString(savedMember));

        // 데이터 검증
        assertThat(savedMember).isNotNull();
        assertThat(savedMember.getEmail()).isEqualTo("test2@gmail.com");
        assertThat(savedMember.getMemberName()).isEqualTo("테스트");
        assertThat(savedMember.getTel()).isEqualTo("010-1234-5678");

        // 기본값 검증
        assertThat(savedMember.getPoint()).isEqualTo(0);  // 기본 포인트
        assertThat(savedMember.getMemberState()).isEqualTo(MemberState.ACTIVE);  // 기본 상태
        assertThat(savedMember.getRole()).isEqualTo(Role.MEMBER);  // 기본 권한
        assertThat(savedMember.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());  // 가입일자
        assertThat(savedMember.getLastPasswordChange()).isBeforeOrEqualTo(LocalDateTime.now());  // 비밀번호 변경일자
        assertThat(savedMember.getLastLogin()).isNull();

        // 테스트 통과 (2024-12-11)
    }

    @Test
    @DisplayName("멤버 찾기")
    @Transactional
    void findMember(){  // 2번 테스트
        MemberResponseDto findMember = memberService.findMember(2L);

        assertThat(findMember.getEmail()).isEqualTo("test@gmail.com");
        assertThat(findMember.getMemberName()).isEqualTo("테스트");
        assertThat(findMember.getTel()).isEqualTo("010-1234-5678");

    }

    @Test
    @DisplayName("멤버 상태 변경")
    @Transactional
    void changeMemberState() {
        Member member = memberRepository.findByEmail("test@gmail.com").orElseThrow();
        member.changeMemberState(MemberState.SUSPENDED);
        assertThat(member.getMemberState()).isEqualTo(MemberState.SUSPENDED);
    }

    private static MemberRequestDto getMemberRequestDto(String email) {
        MemberRequestDto requestDto = new MemberRequestDto();
        requestDto.setEmail(email);
        requestDto.setMemberName("테스트");
        requestDto.setMemberPwd("1234567890");
        requestDto.setTel("010-1234-5678");
        return requestDto;
    }
}