package kwh.cofshop.member.contoller;

import kwh.cofshop.ControllerTestSetting;
import kwh.cofshop.member.dto.request.LoginDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;  // POST 요청
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;  // POST 요청
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

class MemberControllerTest extends ControllerTestSetting {

    @Test
    @DisplayName("컨트롤러 로그인 성공 테스트")
    void loginSuccessTest() throws Exception {  // 3번 테스트

        // 로그인 성공 테스트
        LoginDto loginDto = new LoginDto();
        loginDto.setEmail("test@gmail.com");
        loginDto.setMemberPwd("1234567890");

        mockMvc.perform(post("/api/member/login")
                        .contentType(MediaType.APPLICATION_JSON)  // JSON 데이터 타입 설정
                        .content(objectMapper.writeValueAsString(loginDto)))  // DTO -> JSON 변환
                .andExpect(status().isOk())  // 성공
                .andExpect(jsonPath("$.body.data.email").value("test@gmail.com"))  // email 필드 검증
                .andExpect(jsonPath("$.body.data.accessToken").exists())  // accessToken 필드 존재 여부 확인
                .andExpect(jsonPath("$.body.data.refreshToken").exists())  // refreshToken 필드 존재 여부 확인
                .andDo(print());  // 요청 및 응답 출력
    }

    @Test
    @DisplayName("스프링 엔드포인트 로그인 성공 테스트")
    void SpringEndPointLoginSuccessTest() throws Exception {  // 3번 테스트

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
    }
}