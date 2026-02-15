package kwh.cofshop.item.domain;

import jakarta.persistence.*;
import kwh.cofshop.global.domain.BaseEntity;
import kwh.cofshop.member.domain.Member;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Item extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id", nullable = false, updatable = false)
    private Long id; // 상품 코드 (기본 키)

    @Column(name = "item_name", nullable = false)
    private String itemName; // 상품명

    @Column(nullable = false)
    private int price; // 가격

    @Column(nullable = false)
    private Integer discount; // 상품 할인율(%)

    @Enumerated(EnumType.STRING)
    @Column(name = "item_state", nullable = false)
    private ItemState itemState; // 상품 상태 (1: 판매중, 0: 판매 중단)

    @Column(name = "delivery_fee")
    private Integer deliveryFee; // 배송비

    @Column(nullable = false, length = 100)
    private String origin; // 원산지

    @Column(name = "item_limit")
    private Integer itemLimit; // 수량 제한(한 번에 구입 가능한 개수)

    private Double averageRating; // 초기값

    private Long reviewCount; // 초기값

    //연관관계
    ///////////////////////////////////////////////////////////////////////////////

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL)
    private List<ItemCategory> itemCategories = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Member seller;  // 판매자 정보(이메일)

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemOption> itemOptions = new ArrayList<>(); // 옵션 리스트

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ItemImg> itemImgs = new ArrayList<>();  // 아이템 이미지

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>(); // 리뷰 목록

    @Builder
    public Item(String itemName, Integer price,
                Integer discount, Integer deliveryFee, List<ItemCategory> itemCategories, String origin, Integer itemLimit, Member seller) {
        this.itemName = itemName;
        this.price = price;
        this.discount = discount;
        this.deliveryFee = deliveryFee;
        this.itemCategories = itemCategories;
        this.origin = origin;
        this.itemLimit = itemLimit;
        this.seller = seller;
    }


    @PrePersist
    public void prePersist() {
        this.itemState = ItemState.SELL;
        this.discount = this.discount == null ? 0 : this.discount;
        this.averageRating = 0.0;
        this.reviewCount = 0L;
    }

    // 평균 평점과 리뷰 개수 업데이트 메서드 (스케줄링 과정에서 정합성 유지)
    public void updateReviewStats(double averageRating, long reviewCount) {
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }

    // 리뷰 등록 시 평점 변화
    public void addReviewRating(Long rating) {
        this.averageRating = (this.averageRating * this.reviewCount + rating) / (this.reviewCount + 1);
        this.reviewCount++;
    }

    // 리뷰 수정 시 평점 변화
    public void updateReviewRating(Long oldRating, Long newRating) {
        this.averageRating = (this.averageRating * this.reviewCount - oldRating + newRating) / this.reviewCount;
    }

    // 리뷰 삭제 시 평점 변화
    public void deleteReviewRating(Long rating) {
        if (this.reviewCount <= 1) {
            this.averageRating = 0.0;
            this.reviewCount = 0L;
        } else {
            this.averageRating = (this.averageRating * this.reviewCount - rating) / (this.reviewCount - 1);
            this.reviewCount--;
        }
    }


    public void updateItem(String itemName, Integer price, Integer discount, Integer deliveryFee, String origin, Integer itemLimit) {
        if (itemName != null) {
            this.itemName = itemName;
        }
        if (price != null) {
            this.price = price;
        }
        if (discount != null) {
            this.discount = discount;
        }
        if (deliveryFee != null) {
            this.deliveryFee = deliveryFee;
        }
        if (origin != null) {
            this.origin = origin;
        }
        if (itemLimit != null) {
            this.itemLimit = itemLimit;
        }
    }
    ///// 연관관계 편의 메서드

    // 이미지
    public void addItemImg(ItemImg itemImg) {
        this.itemImgs.add(itemImg);
    }

    // 옵션
    public void addItemOption(ItemOption itemOption) {
        this.itemOptions.add(itemOption);
    }

    public void setSeller(Member seller) {
        this.seller = seller;
        if (!seller.getItemList().contains(this)) {
            seller.getItemList().add(this);
        }
    }
}
