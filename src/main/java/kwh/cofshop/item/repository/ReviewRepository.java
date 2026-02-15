package kwh.cofshop.item.repository;

import kwh.cofshop.item.domain.Review;
import kwh.cofshop.item.repository.query.ReviewRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewRepositoryCustom {

    List<Review> getReviewsByItemId(Long itemId);

    boolean existsByItemIdAndMemberId(Long itemId, Long memberId);

}
