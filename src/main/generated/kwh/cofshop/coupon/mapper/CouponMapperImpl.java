package kwh.cofshop.coupon.mapper;

import javax.annotation.processing.Generated;
import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.dto.response.CouponResponseDto;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-24T00:40:11+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.9 (Oracle Corporation)"
)
@Component
public class CouponMapperImpl implements CouponMapper {

    @Override
    public CouponResponseDto toResponseDto(Coupon coupon) {
        if ( coupon == null ) {
            return null;
        }

        CouponResponseDto couponResponseDto = new CouponResponseDto();

        couponResponseDto.setId( coupon.getId() );
        couponResponseDto.setName( coupon.getName() );
        couponResponseDto.setType( coupon.getType() );
        couponResponseDto.setDiscountValue( coupon.getDiscountValue() );
        if ( coupon.getMinOrderPrice() != null ) {
            couponResponseDto.setMinOrderPrice( coupon.getMinOrderPrice() );
        }
        couponResponseDto.setValidFrom( coupon.getValidFrom() );
        couponResponseDto.setValidTo( coupon.getValidTo() );

        return couponResponseDto;
    }
}
