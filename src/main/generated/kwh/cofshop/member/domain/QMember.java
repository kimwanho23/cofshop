package kwh.cofshop.member.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMember is a Querydsl query type for Member
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMember extends EntityPathBase<Member> {

    private static final long serialVersionUID = -43321664L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMember member = new QMember("member1");

    public final kwh.cofshop.cart.domain.QCart cart;

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath email = createString("email");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<kwh.cofshop.item.domain.Item, kwh.cofshop.item.domain.QItem> itemList = this.<kwh.cofshop.item.domain.Item, kwh.cofshop.item.domain.QItem>createList("itemList", kwh.cofshop.item.domain.Item.class, kwh.cofshop.item.domain.QItem.class, PathInits.DIRECT2);

    public final DateTimePath<java.time.LocalDateTime> lastLogin = createDateTime("lastLogin", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> lastPasswordChange = createDateTime("lastPasswordChange", java.time.LocalDateTime.class);

    public final StringPath memberName = createString("memberName");

    public final StringPath memberPwd = createString("memberPwd");

    public final EnumPath<MemberState> memberState = createEnum("memberState", MemberState.class);

    public final NumberPath<Integer> point = createNumber("point", Integer.class);

    public final ListPath<kwh.cofshop.item.domain.Review, kwh.cofshop.item.domain.QReview> reviews = this.<kwh.cofshop.item.domain.Review, kwh.cofshop.item.domain.QReview>createList("reviews", kwh.cofshop.item.domain.Review.class, kwh.cofshop.item.domain.QReview.class, PathInits.DIRECT2);

    public final EnumPath<Role> role = createEnum("role", Role.class);

    public final StringPath tel = createString("tel");

    public QMember(String variable) {
        this(Member.class, forVariable(variable), INITS);
    }

    public QMember(Path<? extends Member> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMember(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMember(PathMetadata metadata, PathInits inits) {
        this(Member.class, metadata, inits);
    }

    public QMember(Class<? extends Member> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.cart = inits.isInitialized("cart") ? new kwh.cofshop.cart.domain.QCart(forProperty("cart"), inits.get("cart")) : null;
    }

}

