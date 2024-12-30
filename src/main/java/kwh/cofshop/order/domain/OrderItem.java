
package kwh.cofshop.order.domain;

import jakarta.persistence.*;
import kwh.cofshop.global.domain.BaseTimeEntity;
import kwh.cofshop.item.domain.Item;
import lombok.*;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "order_details")
public class OrderItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_detail_id", nullable = false, updatable = false)
    private Long orderDetailId;  // 주문 상세 ID (PK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;  // 주문 번호 (FK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;  // 상품 ID (FK)

    @Column(nullable = false)
    private int quantity;  // 수량

    @Column(name = "item_price", nullable = false)
    private int orderPrice;  // 개별 금액 (상품 + 옵션 가격)

    public int getTotalPrice() {
        return orderPrice * quantity;
    }
}
