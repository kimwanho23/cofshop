package kwh.cofshop.member.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.global.TokenDto;
import kwh.cofshop.member.domain.Role;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.member.dto.LoginDto;
import kwh.cofshop.member.dto.MemberRequestDto;
import kwh.cofshop.member.dto.MemberResponseDto;
import kwh.cofshop.member.mapper.MemberMapper;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.security.CustomUserDetails;
import kwh.cofshop.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;  // POST 요청
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;  // POST 요청
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
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
    void signUp(){

        //DTO 생성 (프론트에서 입력)
        MemberRequestDto requestDto = new MemberRequestDto();
        requestDto.setEmail("test@gmail.com");
        requestDto.setMemberName("테스트");
        requestDto.setMemberPwd("1234567890");
        requestDto.setTel("010-1234-5678");

        // 저장
        Member savedMember = memberService.save(requestDto);

        // 데이터 검증
        assertThat(savedMember).isNotNull();
        assertThat(savedMember.getEmail()).isEqualTo("test@gmail.com");
        assertThat(savedMember.getMemberName()).isEqualTo("테스트");
//        assertThat(savedMember.getMemberPwd()).isEqualTo("1234567890");
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
    void findMember(){
        Long id = 1L; // 멤버 아이디
        MemberResponseDto findMember = memberService.findMember(1L);

        assertThat(findMember.getEmail()).isEqualTo("test@gmail.com");
        assertThat(findMember.getMemberName()).isEqualTo("테스트");
        assertThat(findMember.getTel()).isEqualTo("010-1234-5678");

        // 테스트 통과 (2024-12-11)
    }

    @Test
    @DisplayName("로그인 성공 테스트")
    void loginSuccessTest() throws Exception {

        // 로그인 성공 테스트
        LoginDto loginDto = new LoginDto();
        loginDto.setEmail("test@gmail.com");
        loginDto.setMemberPwd("1234567890");

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)  // JSON 데이터 타입 설정
                        .content(objectMapper.writeValueAsString(loginDto)))  // DTO -> JSON 변환
                .andExpect(status().isOk())  // 성공
                .andExpect(jsonPath("$.email").value("test@gmail.com"))  // 응답 필드 검증
                .andExpect(jsonPath("$.accessToken").exists())  // accessToken
                .andExpect(jsonPath("$.refreshToken").exists())  // refreshToken
                .andDo(print());  // 요청 및 응답 출력

        // 로그인 실패 테스트
        LoginDto loginDto2 = new LoginDto();
        loginDto.setEmail("test@gmail.com");
        loginDto.setMemberPwd("123456789");

        mockMvc.perform(post("/doLogin")
                        .contentType(MediaType.APPLICATION_JSON)  // JSON 데이터 타입 설정
                        .content(objectMapper.writeValueAsString(loginDto)))  // DTO -> JSON 변환
                .andExpect(status().isUnauthorized())  // 401 에러
                .andDo(print());  // 요청 및 응답 출력
    }

    @Test
    @DisplayName("토큰 요청 테스트")
    void expiredJwtTokenTest() throws Exception {
        Member byEmail = memberRepository.findByEmail("test@gmail.com").orElseThrow();

        CustomUserDetails customUserDetails = new CustomUserDetails(byEmail);

        TokenDto token = jwtTokenProvider.createAuthToken(customUserDetails); // 토큰 생성

        mockMvc.perform(get("/api/m/protected")
                        .header("Authorization", "Bearer " + token.getAccessToken()))
                .andExpect(status().isOk())  // 200 응답 확인
                .andExpect(jsonPath("$.body.data").value("보호된 페이지에 접근 성공!"))
                .andDo(print()); // 인증이 필요한 페이지에 접속 테스트
    }



}