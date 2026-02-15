package kwh.cofshop.item.repository.query;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.item.domain.QReview;
import kwh.cofshop.item.domain.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReviewRepositoryCustomImpl implements ReviewRepositoryCustom {


    private final JPAQueryFactory queryFactory;

    @Override
    public List<Review> findByItemId(Long itemId) {
        QReview review = QReview.review;
        return queryFactory.selectFrom(review)
                .where(review.item.id.eq(itemId))
                .fetch();
    }

    @Override
    public Page<Review> findByItemId(Long itemId, Pageable pageable) {
        QReview review = QReview.review;
        List<Review> content = queryFactory.selectFrom(review)
                .where(review.item.id.eq(itemId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(review.createDate.desc()) // ?뺣젹 議곌굔 ?꾩슂 ??
                .fetch();

        long total = Optional.ofNullable(queryFactory.select(review.count())
                .from(review)
                .where(review.item.id.eq(itemId))
                .fetchOne()).orElse(0L);

        return new PageImpl<>(content, pageable, total);
    }
}
