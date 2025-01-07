package kwh.cofshop.cart.domain;

import jakarta.persistence.*;
import kwh.cofshop.member.domain.Member;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart { // 장바구니

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_id")
    private Long cartId;

    @OneToOne
    @JoinColumn(name="member_id", nullable = false)
    private Member member;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> cartItems = new ArrayList<>();

    public Cart(Member member) {
        this.member = member;
    }

    public void addCartItem(CartItem cartItem) {
        if (this.cartItems == null) {
            this.cartItems = new ArrayList<>();
        }
        // 장바구니 항목 추가
        this.cartItems.add(cartItem);

        // 양방향 연관관계 설정
        cartItem.setCart(this);
    }

}
