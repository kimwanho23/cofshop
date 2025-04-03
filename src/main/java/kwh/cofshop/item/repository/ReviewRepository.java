package kwh.cofshop.item.repository;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.Review;
import kwh.cofshop.member.domain.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 특정 Item에 연관된 모든 리뷰 조회
    List<Review> findByItemId(Long itemId);

    // 특정 Item에 연관된 리뷰 페이징
    Page<Review> findByItemId(Long itemId , Pageable pageable);


    // 특정 사용자의 해당 아이템의 리뷰 조회
    Review findByItemAndMember(Item item, Member member);

    boolean existsByItemIdAndMemberId(Long itemId, Long memberId);
}
