
package kwh.cofshop.order.domain;

import jakarta.persistence.*;
import kwh.cofshop.global.domain.BaseTimeEntity;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import lombok.*;

@Getter
@NoArgsConstructor
@Entity
@ToString
@Table(name = "order_Items")
public class OrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id", nullable = false, updatable = false)
    private Long id;  // 주문 상세 ID (PK)

    @Column(nullable = false)
    private int quantity;  // 수량

    @Column(name = "item_price", nullable = false)
    private int orderPrice;  // 개별 금액 (상품 + 옵션 가격)

    /////////////////////////////////////////////////////////////////
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;  // 주문 번호 (FK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;  // 상품 ID (FK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private ItemOption itemOption; // 옵션 정보

    @Builder
    public OrderItem(int quantity, int orderPrice, Order order, Item item, ItemOption itemOption) {
        this.quantity = quantity;
        this.orderPrice = orderPrice;
        this.order = order;
        this.item = item;
        this.itemOption = itemOption;
    }

    public static OrderItem createOrderItem(ItemOption itemOption, int quantity) {
        itemOption.removeStock(quantity);
        return OrderItem.builder()
                .item(itemOption.getItem())
                .itemOption(itemOption)
                .quantity(quantity)
                .orderPrice(itemOption.getTotalPrice() * quantity)
                .build();
    }

    // 재고 복구
    public void restoreStock() {
        this.itemOption.addStock(this.quantity);
    }
}
