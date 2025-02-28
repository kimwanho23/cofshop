package kwh.cofshop.member.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMemberLoginHistory is a Querydsl query type for MemberLoginHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMemberLoginHistory extends EntityPathBase<MemberLoginHistory> {

    private static final long serialVersionUID = 665665483L;

    public static final QMemberLoginHistory memberLoginHistory = new QMemberLoginHistory("memberLoginHistory");

    public final StringPath device = createString("device");

    public final StringPath email = createString("email");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath ipAddress = createString("ipAddress");

    public final DateTimePath<java.time.LocalDateTime> loginDt = createDateTime("loginDt", java.time.LocalDateTime.class);

    public QMemberLoginHistory(String variable) {
        super(MemberLoginHistory.class, forVariable(variable));
    }

    public QMemberLoginHistory(Path<? extends MemberLoginHistory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMemberLoginHistory(PathMetadata metadata) {
        super(MemberLoginHistory.class, metadata);
    }

}

