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
    private Long itemId; // 상품 코드 (기본 키)

    @Column(name = "item_name", nullable = false)
    private String itemName; // 상품명

    @Column(nullable = false)
    private Integer price; // 가격

    @Column(name = "item_state", nullable = false)
    private ItemState itemState; // 상품 상태 (1: 판매중, 0: 판매 중단)

    private Integer discount; // 할인율 (%)

    @Column(name = "delivery_fee")
    private Integer deliveryFee; // 배송비

    @Column(nullable = false, length = 100)
    private String origin; // 원산지

    @Column(name = "item_limit")
    private Integer itemLimit; // 수량 제한(한 번에 구입 가능한 개수)

    private Double averageRating; // 초기값

    private Integer reviewCount; // 초기값

    //연관관계
    ///////////////////////////////////////////////////////////////////////////////

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category; // 상품이 카테고리(FK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_email", referencedColumnName = "email", nullable = false)
    private Member seller;  // 판매자 정보(이메일)

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ItemOption> itemOptions = new ArrayList<>(); // 옵션 리스트

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemImg> itemImgs = new ArrayList<>();  // 컬렉션 타입 사용

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();


    @Builder
    public Item(String itemName, Integer price, Integer discount,
                Integer deliveryFee, Category category, String origin, Integer itemLimit, Member seller) {
        this.itemName = itemName;
        this.price = price;
        this.discount = discount;
        this.deliveryFee = deliveryFee;
        this.category = category;
        this.origin = origin;
        this.itemLimit = itemLimit;
        this.seller = seller;
    }


    @PrePersist
    public void prePersist() {
        this.itemState = ItemState.SELL;
        this.averageRating = 0.0;
        this.reviewCount = 0;
    }

    // 평균 평점과 리뷰 개수 업데이트 메서드
    public void updateReviewStats(double averageRating, int reviewCount) {
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
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
