package kwh.cofshop.coupon.service;

import kwh.cofshop.coupon.domain.CouponType;
import kwh.cofshop.coupon.dto.request.CouponRequestDto;
import kwh.cofshop.coupon.dto.response.CouponResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@AutoConfigureMockMvc
@Slf4j
class CouponServiceTest {

    @Autowired
    private CouponService couponService;

    @Test
    @DisplayName("쿠폰 생성")
    void createCoupon() {
        couponService.createCoupon(getCouponRequestDto());
    }


    //쿠폰 requestDto
    public CouponRequestDto getCouponRequestDto(){
        CouponRequestDto couponRequestDto = new CouponRequestDto();
        couponRequestDto.setType(CouponType.RATE);
        couponRequestDto.setName("쿠폰 1");
        couponRequestDto.setDiscountValue(15);
        couponRequestDto.setMinOrderPrice(0);
        couponRequestDto.setMaxDiscountAmount(5000);
        couponRequestDto.setValidFrom(LocalDate.now());
        couponRequestDto.setValidTo(LocalDate.now().plusDays(150));
        return couponRequestDto;
    }

}