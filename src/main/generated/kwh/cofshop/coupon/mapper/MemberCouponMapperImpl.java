package kwh.cofshop.coupon.mapper;

import javax.annotation.processing.Generated;
import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.coupon.dto.response.MemberCouponResponseDto;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-24T00:40:11+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.9 (Oracle Corporation)"
)
@Component
public class MemberCouponMapperImpl implements MemberCouponMapper {

    @Override
    public MemberCouponResponseDto toResponseDto(MemberCoupon memberCoupon) {
        if ( memberCoupon == null ) {
            return null;
        }

        MemberCouponResponseDto memberCouponResponseDto = new MemberCouponResponseDto();

        memberCouponResponseDto.setState( memberCoupon.getState() );
        memberCouponResponseDto.setIssuedAt( memberCoupon.getIssuedAt() );
        memberCouponResponseDto.setUsedAt( memberCoupon.getUsedAt() );
        memberCouponResponseDto.setExpiredAt( memberCoupon.getExpiredAt() );

        return memberCouponResponseDto;
    }
}
