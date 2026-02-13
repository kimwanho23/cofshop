package kwh.cofshop.coupon.controller;

import kwh.cofshop.coupon.dto.response.MemberCouponResponseDto;
import kwh.cofshop.coupon.service.MemberCouponService;
import kwh.cofshop.support.StandaloneMockMvcFactory;
import kwh.cofshop.support.TestLoginMemberArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MemberCouponControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MemberCouponService memberCouponService;

    @InjectMocks
    private MemberCouponController memberCouponController;

    @BeforeEach
    void setUp() {
        mockMvc = StandaloneMockMvcFactory.build(
                memberCouponController,
                new TestLoginMemberArgumentResolver()
        );
    }

    @Test
    @DisplayName("쿠폰 발급 요청")
    void issueCoupon() throws Exception {
        mockMvc.perform(post("/api/memberCoupon/me/1"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("내 쿠폰 목록 조회")
    void getMemberCouponList() throws Exception {
        when(memberCouponService.memberCouponList(anyLong()))
                .thenReturn(List.of(new MemberCouponResponseDto()));

        mockMvc.perform(get("/api/memberCoupon/me"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("쿠폰 만료 처리")
    void expireMemberCoupons() throws Exception {
        mockMvc.perform(patch("/api/memberCoupon/expire")
                        .param("date", LocalDate.now().toString()))
                .andExpect(status().isNoContent());
    }
}
