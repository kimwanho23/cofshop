package kwh.cofshop.item.repository.custom;

import kwh.cofshop.item.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReviewRepositoryCustom {

    // 특정 Item에 연관된 모든 리뷰 조회
    List<Review> findByItemId(Long itemId);

    // 특정 Item에 연관된 리뷰 페이징
    Page<Review> findByItemId(Long itemId , Pageable pageable);


    // 특정 사용자의 해당 아이템의 리뷰 조회
    Review findByItemAndMember(Long itemId, Long memberId);

    boolean existsByItemIdAndMemberId(Long itemId, Long memberId);
}
