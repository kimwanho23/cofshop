package kwh.cofshop.item.repository;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.Review;
import kwh.cofshop.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 특정 Item에 연관된 모든 리뷰 조회
    List<Review> findByItem(Item item);


    // 특정 사용자의 해당 아이템의 리뷰 조회
    Review findByItemAndMember(Item item, Member member);
}
