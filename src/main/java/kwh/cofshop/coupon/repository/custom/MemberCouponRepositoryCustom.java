package kwh.cofshop.coupon.repository.custom;

import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.domain.MemberCoupon;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MemberCouponRepositoryCustom {

    Optional<MemberCoupon> findValidCouponById(Long couponId, Long memberId, LocalDate today);

    List<MemberCoupon> findByCouponExpired(CouponState state, LocalDate date);
}
