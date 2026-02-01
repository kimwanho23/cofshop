package kwh.cofshop.coupon.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.domain.CouponType;
import kwh.cofshop.coupon.dto.request.CouponRequestDto;
import kwh.cofshop.coupon.dto.response.CouponResponseDto;
import kwh.cofshop.coupon.service.CouponService;
import kwh.cofshop.support.StandaloneMockMvcFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CouponControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private CouponService couponService;

    @InjectMocks
    private CouponController couponController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = StandaloneMockMvcFactory.build(couponController);
    }

    @Test
    @DisplayName("쿠폰 단건 조회")
    void getCouponById() throws Exception {
        when(couponService.getCouponById(anyLong())).thenReturn(new CouponResponseDto());

        mockMvc.perform(get("/api/coupon/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("쿠폰 전체 조회")
    void getAllCoupons() throws Exception {
        when(couponService.getAllCoupons()).thenReturn(List.of(new CouponResponseDto()));

        mockMvc.perform(get("/api/coupon"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("쿠폰 생성")
    void createCoupon() throws Exception {
        when(couponService.createCoupon(any())).thenReturn(1L);

        CouponRequestDto requestDto = new CouponRequestDto();
        requestDto.setName("테스트 쿠폰");
        requestDto.setType(CouponType.FIXED);
        requestDto.setDiscountValue(1000);
        requestDto.setValidFrom(LocalDate.now());
        requestDto.setValidTo(LocalDate.now().plusDays(10));

        mockMvc.perform(post("/api/coupon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("쿠폰 상태 변경")
    void updateCouponState() throws Exception {
        mockMvc.perform(patch("/api/coupon/1/state")
                        .param("newState", CouponState.CANCELLED.name()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("쿠폰 취소")
    void cancelCoupon() throws Exception {
        mockMvc.perform(patch("/api/coupon/1/cancel"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("쿠폰 만료 처리")
    void expireCoupons() throws Exception {
        mockMvc.perform(post("/api/coupon/expire")
                        .param("date", LocalDate.now().toString()))
                .andExpect(status().isNoContent());
    }
}
