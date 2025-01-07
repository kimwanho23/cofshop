
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
    private Long orderDetailId;  // 주문 상세 ID (PK)

    @Column(nullable = false)
    private int quantity;  // 수량

    @Column(name = "item_price", nullable = false)
    private int orderPrice;  // 개별 금액 (상품 + 옵션 가격)

    /////////////////////////////////////////////////////////////////
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

    public void setOrder(Order order){
        this.order = order;
    }

    public static OrderItem createOrderItem(Item item, ItemOption itemOption, int quantity) {
        OrderItem orderItem = OrderItem.builder()
                .item(item)
                .itemOption(itemOption)
                .quantity(quantity)
                .orderPrice(item.getPrice() + itemOption.getAdditionalPrice())
                .build();
        return orderItem;
    }


    public int getTotalPrice() { // 총 가격
        return orderPrice * quantity;
    }
}
