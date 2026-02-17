package kwh.cofshop.item.repository.query;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kwh.cofshop.item.domain.QReview;
import kwh.cofshop.item.dto.response.ReviewResponseDto;
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
    public List<ReviewResponseDto> findReviewResponsesByItemId(Long itemId) {
        QReview review = QReview.review;

        return queryFactory
                .select(Projections.fields(
                        ReviewResponseDto.class,
                        review.id.as("reviewId"),
                        review.member.id.as("memberId"),
                        review.rating,
                        review.content,
                        review.item.id.as("itemId")
                ))
                .from(review)
                .where(review.item.id.eq(itemId))
                .orderBy(review.createDate.desc())
                .fetch();
    }

    @Override
    public Page<ReviewResponseDto> findReviewResponsesByItemId(Long itemId, Pageable pageable) {
        QReview review = QReview.review;

        List<ReviewResponseDto> content = queryFactory
                .select(Projections.fields(
                        ReviewResponseDto.class,
                        review.id.as("reviewId"),
                        review.member.id.as("memberId"),
                        review.rating,
                        review.content,
                        review.item.id.as("itemId")
                ))
                .from(review)
                .where(review.item.id.eq(itemId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(review.createDate.desc())
                .fetch();

        long total = Optional.ofNullable(queryFactory
                        .select(review.count())
                        .from(review)
                        .where(review.item.id.eq(itemId))
                        .fetchOne())
                .orElse(0L);

        return new PageImpl<>(content, pageable, total);
    }
}
