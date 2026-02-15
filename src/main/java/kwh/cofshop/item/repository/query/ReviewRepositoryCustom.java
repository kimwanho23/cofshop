package kwh.cofshop.item.repository.query;

import kwh.cofshop.item.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReviewRepositoryCustom {

    List<Review> findByItemId(Long itemId);

    Page<Review> findByItemId(Long itemId, Pageable pageable);
}
