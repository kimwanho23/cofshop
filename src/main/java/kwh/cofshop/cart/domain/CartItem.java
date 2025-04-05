package kwh.cofshop.cart.domain;

import jakarta.persistence.*;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private Long id;

    private int quantity;  // 수량

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private ItemOption itemOption;

    @Builder
    public CartItem(int quantity, Cart cart, Item item, ItemOption itemOption) {
        this.quantity = quantity;
        this.cart = cart;
        this.item = item;
        this.itemOption = itemOption;
    }

    // 수량 증가
    public void addQuantity(int additionalQuantity) {
        this.quantity += additionalQuantity;
    }

    // Cart 설정 (양방향 연관관계)
    protected void setCart(Cart cart) {
        this.cart = cart;
    }

    // 수량 변경
    public void changeQuantity(int newQuantity) {
        this.quantity = newQuantity;
    }

    // 옵션 번호
    public int getOptionNo() {
        return itemOption.getOptionNo();
    }

    // 총 가격
    public int getTotalPrice() {
        return (item.getPrice() + itemOption.getAdditionalPrice()) * quantity;
    }
}
