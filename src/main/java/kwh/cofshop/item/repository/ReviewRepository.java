package kwh.cofshop.item.repository;

import kwh.cofshop.item.domain.Review;
import kwh.cofshop.item.repository.custom.ReviewRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewRepositoryCustom {

    List<Review> getReviewsByItemId(Long itemId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.item.id = :itemId")
    Double findAverageRatingByItemId(@Param("itemId") Long itemId);

    // 숫자 조회
    long countByItemId(Long itemId);

}
