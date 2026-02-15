package kwh.cofshop.item.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QItem is a Querydsl query type for Item
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QItem extends EntityPathBase<Item> {

    private static final long serialVersionUID = -1073809486L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QItem item = new QItem("item");

    public final kwh.cofshop.global.domain.QBaseEntity _super = new kwh.cofshop.global.domain.QBaseEntity(this);

    public final NumberPath<Double> averageRating = createNumber("averageRating", Double.class);

    //inherited
    public final StringPath createBy = _super.createBy;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createDate = _super.createDate;

    public final NumberPath<Integer> deliveryFee = createNumber("deliveryFee", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<ItemCategory, QItemCategory> itemCategories = this.<ItemCategory, QItemCategory>createList("itemCategories", ItemCategory.class, QItemCategory.class, PathInits.DIRECT2);

    public final ListPath<ItemImg, QItemImg> itemImgs = this.<ItemImg, QItemImg>createList("itemImgs", ItemImg.class, QItemImg.class, PathInits.DIRECT2);

    public final NumberPath<Integer> itemLimit = createNumber("itemLimit", Integer.class);

    public final StringPath itemName = createString("itemName");

    public final ListPath<ItemOption, QItemOption> itemOptions = this.<ItemOption, QItemOption>createList("itemOptions", ItemOption.class, QItemOption.class, PathInits.DIRECT2);

    public final EnumPath<ItemState> itemState = createEnum("itemState", ItemState.class);

    //inherited
    public final StringPath lastModifiedBy = _super.lastModifiedBy;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> lastModifiedDate = _super.lastModifiedDate;

    public final StringPath origin = createString("origin");

    public final NumberPath<Integer> price = createNumber("price", Integer.class);

    public final NumberPath<Long> reviewCount = createNumber("reviewCount", Long.class);

    public final ListPath<Review, QReview> reviews = this.<Review, QReview>createList("reviews", Review.class, QReview.class, PathInits.DIRECT2);

    public final kwh.cofshop.member.domain.QMember seller;

    public QItem(String variable) {
        this(Item.class, forVariable(variable), INITS);
    }

    public QItem(Path<? extends Item> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QItem(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QItem(PathMetadata metadata, PathInits inits) {
        this(Item.class, metadata, inits);
    }

    public QItem(Class<? extends Item> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.seller = inits.isInitialized("seller") ? new kwh.cofshop.member.domain.QMember(forProperty("seller"), inits.get("seller")) : null;
    }

}

