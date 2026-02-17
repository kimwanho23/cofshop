package kwh.cofshop.item.repository.query;

import kwh.cofshop.item.dto.response.ReviewResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReviewRepositoryCustom {

    List<ReviewResponseDto> findReviewResponsesByItemId(Long itemId);

    Page<ReviewResponseDto> findReviewResponsesByItemId(Long itemId, Pageable pageable);
}
