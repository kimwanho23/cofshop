package kwh.cofshop.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.member.domain.MemberState;
import kwh.cofshop.member.dto.request.MemberPasswordUpdateRequestDto;
import kwh.cofshop.member.dto.request.MemberRequestDto;
import kwh.cofshop.member.dto.response.MemberResponseDto;
import kwh.cofshop.member.event.MemberLoginEvent;
import kwh.cofshop.member.service.MemberLoginHistoryService;
import kwh.cofshop.member.service.MemberService;
import kwh.cofshop.support.StandaloneMockMvcFactory;
import kwh.cofshop.support.TestLoginMemberArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private MemberService memberService;

    @Mock
    private MemberLoginHistoryService memberLoginHistoryService;

    @InjectMocks
    private MemberController memberController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = StandaloneMockMvcFactory.build(
                memberController,
                new TestLoginMemberArgumentResolver()
        );
    }

    @Test
    @DisplayName("회원 정보 조회")
    void getMemberById() throws Exception {
        when(memberService.findMember(anyLong())).thenReturn(new MemberResponseDto());

        mockMvc.perform(get("/api/members/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("회원 목록 조회")
    void getAllMembers() throws Exception {
        when(memberService.memberLists()).thenReturn(List.of(new MemberResponseDto()));

        mockMvc.perform(get("/api/members"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그인 이력 조회")
    void getLoginMemberHistory() throws Exception {
        when(memberLoginHistoryService.getUserLoginHistory(anyLong()))
                .thenReturn(List.of(MemberLoginEvent.builder()
                        .memberId(1L)
                        .loginDt(LocalDateTime.now())
                        .build()));

        mockMvc.perform(get("/api/members/history/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("회원 가입")
    void signup() throws Exception {
        MemberResponseDto responseDto = new MemberResponseDto();
        responseDto.setMemberId(1L);

        when(memberService.signUp(any())).thenReturn(responseDto);

        MemberRequestDto requestDto = new MemberRequestDto();
        requestDto.setEmail("user@example.com");
        requestDto.setMemberName("사용자");
        requestDto.setMemberPwd("password123");
        requestDto.setTel("01012341234");

        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("관리자 회원 상태 변경")
    void updateMemberStateByAdmin() throws Exception {
        mockMvc.perform(patch("/api/members/1/state")
                        .param("memberState", MemberState.ACTIVE.name()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("회원 탈퇴")
    void quitMember() throws Exception {
        mockMvc.perform(patch("/api/members/me/state"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("비밀번호 변경")
    void changePassword() throws Exception {
        MemberPasswordUpdateRequestDto requestDto = new MemberPasswordUpdateRequestDto();
        requestDto.setPassword("newPassword");

        mockMvc.perform(patch("/api/members/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("포인트 변경")
    void updatePoint() throws Exception {
        when(memberService.updatePoint(anyLong(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(100);

        mockMvc.perform(patch("/api/members/1/point")
                        .param("amount", "100"))
                .andExpect(status().isOk());
    }
}
