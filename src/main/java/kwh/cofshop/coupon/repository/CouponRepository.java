package kwh.cofshop.coupon.repository;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.repository.custom.CouponRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long>, CouponRepositoryCustom {

    List<Coupon> findByValidToBeforeAndState(LocalDate date, CouponState state);
}
