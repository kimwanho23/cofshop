package kwh.cofshop.payment.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPaymentEntity is a Querydsl query type for PaymentEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPaymentEntity extends EntityPathBase<PaymentEntity> {

    private static final long serialVersionUID = 1171307671L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPaymentEntity paymentEntity = new QPaymentEntity("paymentEntity");

    public final StringPath buyerEmail = createString("buyerEmail");

    public final StringPath buyerName = createString("buyerName");

    public final StringPath buyerTel = createString("buyerTel");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath impUid = createString("impUid");

    public final kwh.cofshop.member.domain.QMember member;

    public final StringPath merchantUid = createString("merchantUid");

    public final kwh.cofshop.order.domain.QOrder order;

    public final NumberPath<Long> paidAmount = createNumber("paidAmount", Long.class);

    public final DateTimePath<java.time.LocalDateTime> paidAt = createDateTime("paidAt", java.time.LocalDateTime.class);

    public final StringPath payMethod = createString("payMethod");

    public final StringPath pgProvider = createString("pgProvider");

    public final StringPath pgTid = createString("pgTid");

    public final NumberPath<Long> price = createNumber("price", Long.class);

    public final DateTimePath<java.time.LocalDateTime> requestedAt = createDateTime("requestedAt", java.time.LocalDateTime.class);

    public final EnumPath<PaymentStatus> status = createEnum("status", PaymentStatus.class);

    public QPaymentEntity(String variable) {
        this(PaymentEntity.class, forVariable(variable), INITS);
    }

    public QPaymentEntity(Path<? extends PaymentEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPaymentEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPaymentEntity(PathMetadata metadata, PathInits inits) {
        this(PaymentEntity.class, metadata, inits);
    }

    public QPaymentEntity(Class<? extends PaymentEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.member = inits.isInitialized("member") ? new kwh.cofshop.member.domain.QMember(forProperty("member"), inits.get("member")) : null;
        this.order = inits.isInitialized("order") ? new kwh.cofshop.order.domain.QOrder(forProperty("order"), inits.get("order")) : null;
    }

}

