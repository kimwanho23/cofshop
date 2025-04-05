package kwh.cofshop.item.domain;

import jakarta.persistence.*;
import kwh.cofshop.global.domain.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name="item_img")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemImg extends BaseTimeEntity {

    @Id
    @Column(name="item_img_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String imgName; //이미지 파일명

    private String oriImgName; //원본 이미지 파일명

    private String imgUrl; //이미지 경로

    @Enumerated(EnumType.STRING)
    private ImgType imgType; //대표 이미지 여부

    ///////////////////////////////////////////////////////////////////////////////

    @ManyToOne(fetch = FetchType.LAZY)
    private Item item; // 연관된 상품 ID

    @Builder
    public ItemImg(Long id, String imgName, String oriImgName, String imgUrl, ImgType imgType, Item item) {
        this.id = id;
        this.imgName = imgName;
        this.oriImgName = oriImgName;
        this.imgUrl = imgUrl;
        this.imgType = imgType;
        this.item = item;
    }

    // 정적 팩토리 메소드
    public static ItemImg createImg(String imgName, String oriImgName, String imgUrl, ImgType imgType, Item item) {
        ItemImg itemImg = ItemImg.builder()
                .imgName(imgName)
                .oriImgName(oriImgName)
                .imgUrl(imgUrl)
                .imgType(imgType)
                .item(item)
                .build();
        item.addItemImg(itemImg); // 연관 관계 설정
        return itemImg;
    }
}
