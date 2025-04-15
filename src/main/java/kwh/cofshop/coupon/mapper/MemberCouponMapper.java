package kwh.cofshop.coupon.mapper;

import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.coupon.dto.response.MemberCouponResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MemberCouponMapper {

    MemberCouponResponseDto toResponseDto(MemberCoupon memberCoupon);
}
