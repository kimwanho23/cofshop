package kwh.cofshop.coupon.infrastructure.persistence.repository.custom;

import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.domain.MemberCoupon;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MemberCouponRepositoryCustom {

    Optional<MemberCoupon> findValidCouponByMemberCouponId(Long memberCouponId, Long memberId, LocalDate today);

    List<MemberCoupon> findByCouponExpired(CouponState state, LocalDate date);


}
