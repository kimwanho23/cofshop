package kwh.cofshop.item.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QItemImg is a Querydsl query type for ItemImg
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QItemImg extends EntityPathBase<ItemImg> {

    private static final long serialVersionUID = -941903183L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QItemImg itemImg = new QItemImg("itemImg");

    public final kwh.cofshop.global.domain.QBaseTimeEntity _super = new kwh.cofshop.global.domain.QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDate = _super.createDate;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imgName = createString("imgName");

    public final EnumPath<ImgType> imgType = createEnum("imgType", ImgType.class);

    public final StringPath imgUrl = createString("imgUrl");

    public final QItem item;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> lastModifiedDate = _super.lastModifiedDate;

    public final StringPath oriImgName = createString("oriImgName");

    public QItemImg(String variable) {
        this(ItemImg.class, forVariable(variable), INITS);
    }

    public QItemImg(Path<? extends ItemImg> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QItemImg(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QItemImg(PathMetadata metadata, PathInits inits) {
        this(ItemImg.class, metadata, inits);
    }

    public QItemImg(Class<? extends ItemImg> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.item = inits.isInitialized("item") ? new QItem(forProperty("item"), inits.get("item")) : null;
    }

}

