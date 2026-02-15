package kwh.cofshop.coupon.mapper;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.domain.CouponType;
import kwh.cofshop.coupon.dto.response.CouponResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CouponMapperTest {

    private final CouponMapper couponMapper = Mappers.getMapper(CouponMapper.class);

    @Test
    @DisplayName("Coupon??renamed ?�드가 CouponResponseDto??매핑?�다")
    void toResponseDto_mapsRenamedFields() {
        Coupon coupon = Coupon.builder()
                .id(1L)
                .name("?�컴 쿠폰")
                .type(CouponType.FIXED)
                .state(CouponState.AVAILABLE)
                .discountValue(1000)
                .maxDiscount(3000)
                .couponCreatedAt(LocalDate.of(2026, 2, 14))
                .validFrom(LocalDate.of(2026, 2, 14))
                .validTo(LocalDate.of(2026, 3, 14))
                .build();

        CouponResponseDto responseDto = couponMapper.toResponseDto(coupon);

        assertThat(responseDto.getMaxDiscountAmount()).isEqualTo(3000);
        assertThat(responseDto.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 2, 14, 0, 0));
    }
}
