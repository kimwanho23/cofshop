package kwh.cofshop.coupon.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import static org.junit.jupiter.api.Assertions.*;
import kwh.cofshop.TestSettingUtils;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;  // POST 요청
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;


class MemberCouponControllerTest extends TestSettingUtils  {
    @Test
    @DisplayName("정상적으로 쿠폰 발급 요청을 보낼 수 있다")
    @Transactional
    void requestCouponIssue() throws Exception {

        Long couponId = 44L;

        mockMvc.perform(post("/api/memberCoupon/me/{couponId}", couponId)
                        .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + getToken()))
                .andExpect(status().isCreated())
                .andDo(print())
                .andReturn();

        Thread.sleep(500); // or CountDownLatch.await()
    }
}