
package kwh.cofshop.order.domain;


import jakarta.persistence.*;
import kwh.cofshop.global.domain.BaseTimeEntity;
import kwh.cofshop.member.domain.Member;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="orders")
public class Order extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id", nullable = false, updatable = false)
    private Long id;  // 주문 번호 (PK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;  // 주문자 (FK)

    @Column(nullable = false)
    private Address address;  // 주소지

    @Enumerated(EnumType.STRING)
    @Column(name = "order_state", nullable = false)
    private OrderState orderState;  // 주문 상태 (ENUM)

    private LocalDateTime orderDate; //주문 날짜

    // 연관 관계 설정
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Builder
    public Order(Member member, Address address, OrderState orderState, LocalDateTime orderDate, List<OrderItem> orderItems) {
        this.member = member;
        this.address = address;
        this.orderState = orderState;
        this.orderDate = orderDate;
        this.orderItems = orderItems;
    }

    // 주문 생성
    public static Order createOrder(Member member, Address address, List<OrderItem> orderItems){
        Order order = Order.builder()
                .member(member)
                .address(address)
                .orderState(OrderState.NEW)
                .orderDate(LocalDateTime.now())
                .orderItems(new ArrayList<>())
                .build();
        for (OrderItem orderItem : orderItems) {
            order.addOrderItem(orderItem);
        }
        return order;
    }

    // 주문 상태 변경
    public void changeOrderState(OrderState orderState){
        this.orderState = orderState;
    }


    // 연관관계 편의 메서드
    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        if (orderItem.getOrder() != this) {
            orderItem.setOrder(this);
        }
    }
}