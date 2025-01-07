package kwh.cofshop.item.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kwh.cofshop.global.domain.BaseTimeEntity;
import kwh.cofshop.member.domain.Member;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "unique_member_item", columnNames = {"member_email", "item_id"})
        }
)
public class Review extends BaseTimeEntity { // 리뷰 엔티티 : 1명당 1개의 리뷰, 별점 설정.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId; // 식별자

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Long rating; // 별점

    @Column(nullable = false)
    private String content; // 후기글

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_email", referencedColumnName = "email", nullable = false)
    private Member member; // 작성자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item; // 아이템

    @Builder
    public Review(Long reviewId, Long rating, String content, Member member, Item item) {
        this.reviewId = reviewId;
        this.rating = rating;
        this.content = content;
        this.member = member;
        this.item = item;
    }

    // 정적 팩토리 메서드
    public static Review createReview(Long rating, String content, Member member, Item item) {
        Review review = new Review();
        review.rating = rating;
        review.content = content;
        review.setMember(member);
        review.setItem(item);
        return review;
    }

    // 연관관계 편의 메서드
    public void setMember(Member member) {
        this.member = member;
        if (!member.getReviews().contains(this)) {
            member.getReviews().add(this);
        }
    }

    public void setItem(Item item) {
        this.item = item;
        if (!item.getReviews().contains(this)) {
            item.getReviews().add(this);
        }
    }
}
