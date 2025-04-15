package kwh.cofshop.coupon.mapper;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.dto.response.CouponResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CouponMapper {

    CouponResponseDto toResponseDto(Coupon coupon);
}
