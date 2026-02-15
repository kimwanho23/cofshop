package kwh.cofshop.coupon.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QCoupon is a Querydsl query type for Coupon
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCoupon extends EntityPathBase<Coupon> {

    private static final long serialVersionUID = -547427112L;

    public static final QCoupon coupon = new QCoupon("coupon");

    public final NumberPath<Integer> couponCount = createNumber("couponCount", Integer.class);

    public final DatePath<java.time.LocalDate> couponCreatedAt = createDate("couponCreatedAt", java.time.LocalDate.class);

    public final NumberPath<Integer> discountValue = createNumber("discountValue", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> maxDiscount = createNumber("maxDiscount", Integer.class);

    public final NumberPath<Integer> minOrderPrice = createNumber("minOrderPrice", Integer.class);

    public final StringPath name = createString("name");

    public final EnumPath<CouponState> state = createEnum("state", CouponState.class);

    public final EnumPath<CouponType> type = createEnum("type", CouponType.class);

    public final DatePath<java.time.LocalDate> validFrom = createDate("validFrom", java.time.LocalDate.class);

    public final DatePath<java.time.LocalDate> validTo = createDate("validTo", java.time.LocalDate.class);

    public QCoupon(String variable) {
        super(Coupon.class, forVariable(variable));
    }

    public QCoupon(Path<? extends Coupon> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCoupon(PathMetadata metadata) {
        super(Coupon.class, metadata);
    }

}

