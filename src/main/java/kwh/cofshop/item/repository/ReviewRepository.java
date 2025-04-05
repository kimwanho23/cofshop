package kwh.cofshop.item.repository;

import kwh.cofshop.item.domain.Review;
import kwh.cofshop.item.repository.custom.ReviewRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewRepositoryCustom {


}
