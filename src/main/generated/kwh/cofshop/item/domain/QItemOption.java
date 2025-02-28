package kwh.cofshop.item.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QItemOption is a Querydsl query type for ItemOption
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QItemOption extends EntityPathBase<ItemOption> {

    private static final long serialVersionUID = -1041342777L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QItemOption itemOption = new QItemOption("itemOption");

    public final NumberPath<Integer> additionalPrice = createNumber("additionalPrice", Integer.class);

    public final StringPath description = createString("description");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QItem item;

    public final NumberPath<Integer> optionNo = createNumber("optionNo", Integer.class);

    public final EnumPath<OptionState> optionState = createEnum("optionState", OptionState.class);

    public final NumberPath<Integer> stock = createNumber("stock", Integer.class);

    public QItemOption(String variable) {
        this(ItemOption.class, forVariable(variable), INITS);
    }

    public QItemOption(Path<? extends ItemOption> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QItemOption(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QItemOption(PathMetadata metadata, PathInits inits) {
        this(ItemOption.class, metadata, inits);
    }

    public QItemOption(Class<? extends ItemOption> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.item = inits.isInitialized("item") ? new QItem(forProperty("item"), inits.get("item")) : null;
    }

}

