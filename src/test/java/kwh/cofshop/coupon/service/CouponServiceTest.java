package kwh.cofshop.coupon.service;

import kwh.cofshop.TestSettingUtils;
import kwh.cofshop.coupon.domain.CouponType;
import kwh.cofshop.coupon.dto.request.CouponRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
class CouponServiceTest extends TestSettingUtils {

    @Autowired
    private CouponService couponService;

    @Test
    @DisplayName("쿠폰 생성")
    void createCoupon() {
        couponService.createCoupon(getCouponRequestDtoWithCount());
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

    public CouponRequestDto getCouponRequestDtoWithCount(){
        CouponRequestDto couponRequestDto = new CouponRequestDto();
        couponRequestDto.setType(CouponType.RATE);
        couponRequestDto.setName("쿠폰 1");
        couponRequestDto.setDiscountValue(15);
        couponRequestDto.setMinOrderPrice(0);
        couponRequestDto.setMaxDiscountAmount(5000);
        couponRequestDto.setValidFrom(LocalDate.now());
        couponRequestDto.setValidTo(LocalDate.now().plusDays(150));
        couponRequestDto.setCouponCount(10000);
        return couponRequestDto;
    }

}