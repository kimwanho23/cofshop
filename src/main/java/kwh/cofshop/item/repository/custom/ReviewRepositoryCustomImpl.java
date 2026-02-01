package kwh.cofshop.item.repository.custom;

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
                .orderBy(review.createDate.desc()) // 정렬 조건 필요 시
                .fetch();

        long total = Optional.ofNullable(queryFactory.select(review.count())
                .from(review)
                .where(review.item.id.eq(itemId))
                .fetchOne()).orElse(0L);

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Review findByItemAndMember(Long itemId, Long memberId) {
        QReview review = QReview.review;
        return queryFactory.selectFrom(review)
                .where(
                        review.item.id.eq(itemId),
                        review.member.id.eq(memberId)
                )
                .fetchOne();
    }

    @Override
    public Double findAverageRatingByItemId(Long itemId) {
        QReview review = QReview.review;

        return queryFactory
                .select(review.rating.avg())
                .from(review)
                .where(review.item.id.eq(itemId))
                .fetchOne();
    }
}
