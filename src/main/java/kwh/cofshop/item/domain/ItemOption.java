package kwh.cofshop.item.domain;

import jakarta.persistence.*;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.item.dto.request.ItemOptionRequestDto;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name="item_option")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String description; // 옵션 내용

    @Column
    private Integer additionalPrice; // 추가금 (기본금에 더해서)

    @Column
    private Integer optionNo;

    @Column(nullable = false)
    private Integer stock; // 옵션별 재고

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OptionState optionState; // 옵션 활성화 여부


    ///////////////////////////////////////////////////////////////////////////////

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item; // 연관된 상품 ID

    @PrePersist
    public void prePersist() {
        this.optionState = OptionState.SELL; // 초기엔 전부 판매
    }

    @Builder
    public ItemOption(Long id, String description, Integer additionalPrice, Integer optionNo, Integer stock, OptionState optionState, Item item) {
        this.id = id;
        this.description = description;
        this.additionalPrice = additionalPrice;
        this.optionNo = optionNo;
        this.stock = stock;
        this.optionState = optionState;
        this.item = item;
    }

    // 정적 팩토리 메서드
    public static ItemOption createOption(String description, Integer additionalPrice, Integer optionNo, Integer stock, Item item) {
        ItemOption itemOption = ItemOption.builder()
                .description(description)
                .additionalPrice(additionalPrice)
                .optionNo(optionNo)
                .stock(stock)
                .optionState(OptionState.SELL) // 기본 상태 설정
                .item(item) // 연관 관계 설정
                .build();

        item.addItemOption(itemOption); // 연관관계 설정
        return itemOption;
    }

    public void updateOption(ItemOptionRequestDto dto) {
        this.description = dto.getDescription();
        this.additionalPrice = dto.getAdditionalPrice();
        this.optionNo = dto.getOptionNo();
        this.stock = dto.getStock();
    }

    public void addStock(int stock){ // 재고 더하기 (주문 취소, 재고 추가)
        this.stock += stock;
    }

    public void removeStock(int stock){ // 재고 감소 (주문, 재고 조정)
        if (this.stock < stock) {
            throw new BusinessException(BusinessErrorCode.OUT_OF_STOCK);
        }
        this.stock -= stock;
    }
}
