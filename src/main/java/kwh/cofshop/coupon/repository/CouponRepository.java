package kwh.cofshop.coupon.repository;

import kwh.cofshop.coupon.domain.Coupon;
import kwh.cofshop.coupon.domain.CouponState;
import kwh.cofshop.coupon.repository.custom.CouponRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long>, CouponRepositoryCustom {

    List<Coupon> findByValidToBeforeAndState(LocalDate date, CouponState state);


    @Modifying(clearAutomatically = true)
    @Query("UPDATE Coupon c SET c.state = :expiredState WHERE c.validTo < :now")
    int bulkExpireCoupons(@Param("now") LocalDate now,
                          @Param("expiredState") String expiredState);

    @Modifying
    @Query("UPDATE Coupon c SET c.couponCount = c.couponCount - 1 WHERE c.id = :id AND c.couponCount > 0")
    void decreaseCouponCount(@Param("id") Long couponId);
}
