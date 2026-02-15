package kwh.cofshop.coupon.presentation.mapper;

import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.coupon.presentation.dto.response.MemberCouponResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(
        componentModel = SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MemberCouponMapper {

    @Mapping(source = "id", target = "memberCouponId")
    @Mapping(source = "member.id", target = "memberId")
    @Mapping(source = "coupon.id", target = "couponId")
    MemberCouponResponseDto toResponseDto(MemberCoupon memberCoupon);
}
