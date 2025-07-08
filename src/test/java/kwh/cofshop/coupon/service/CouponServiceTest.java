package kwh.cofshop.coupon.service;

import kwh.cofshop.TestSettingUtils;
import kwh.cofshop.coupon.domain.CouponType;
import kwh.cofshop.coupon.dto.request.CouponRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
class CouponServiceTest extends TestSettingUtils {

    @Autowired
    private CouponService couponService;

    @Test
    @DisplayName("쿠폰 생성")
    @Commit
    void createCoupon() {
        Long coupon1 = couponService.createCoupon(getCouponRequestDtoWithCount());
    }

    public CouponRequestDto getCouponRequestDtoWithCount(){
        CouponRequestDto limitedCoupon = new CouponRequestDto();
        limitedCoupon.setName("신규 할인 쿠폰");
        limitedCoupon.setType(CouponType.FIXED);              // 고정 할인
        limitedCoupon.setDiscountValue(5000);                  // 5,000원 할인
        limitedCoupon.setMaxDiscountAmount(null);              // 최대 할인 금액 없음
        limitedCoupon.setMinOrderPrice(20000);                  // 최소 주문 금액 20,000원 이상
        limitedCoupon.setCouponCount(null);                      // 총 500000장 발급 가능
        limitedCoupon.setValidFrom(LocalDate.now());            // 오늘부터 사용 가능
        limitedCoupon.setValidTo(LocalDate.now().plusMonths(1)); // 1개월간 유효
        return limitedCoupon;

    }

}