
package kwh.cofshop.order.domain;

import jakarta.persistence.*;
import kwh.cofshop.coupon.domain.MemberCoupon;
import kwh.cofshop.global.domain.BaseTimeEntity;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.payment.domain.PaymentEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="orders")
public class Order extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id", nullable = false, updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // 주문 정보
    @Column(nullable = false)
    private String merchantUid;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_state", nullable = false)
    private OrderState orderState;

    // 배송 정보
    @Column(nullable = false)
    private Address address;

    private String deliveryRequest;

    private int deliveryFee;

    // 비용
    @Column(nullable = false)
    private int totalPrice;

    private int discountFromCoupon;

    private Integer usePoint;

    private int finalPrice;

    // 쿠폰
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_coupon_id")
    private MemberCoupon memberCoupon;

    // 연관 관계
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private PaymentEntity payment;

    @Builder
    public Order(Long id, Address address, Integer usePoint, int totalPrice, int finalPrice, int deliveryFee,
                 String merchantUid, LocalDateTime orderDate, String deliveryRequest, MemberCoupon memberCoupon, int discountFromCoupon,
                 Member member, OrderState orderState, List<OrderItem> orderItems, PaymentEntity payment) {
        this.id = id;
        this.address = address;
        this.usePoint = usePoint;
        this.totalPrice = totalPrice;
        this.finalPrice = finalPrice;
        this.deliveryFee = deliveryFee;
        this.merchantUid = merchantUid;
        this.orderDate = orderDate;
        this.deliveryRequest = deliveryRequest;
        this.memberCoupon = memberCoupon;
        this.discountFromCoupon = discountFromCoupon;
        this.member = member;
        this.orderState = orderState;
        this.orderItems = orderItems;
        this.payment = payment;
    }

    // 주문 생성
    public static Order createOrder(Member member, Address address, List<OrderItem> orderItems){
        Order order = Order.builder()
                .member(member)
                .address(address)
                .merchantUid(UUID.randomUUID() + "TEST")
                .orderState(OrderState.NEW)
                .orderDate(LocalDateTime.now())
                .orderItems(new ArrayList<>())
                .totalPrice(orderItems.stream()
                                .mapToInt(OrderItem::getTotalPrice)
                                .sum()
                )
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


    // 사용할 쿠폰 등록
    public void addUseCoupon(MemberCoupon memberCoupon, int discountAmount) {
        this.memberCoupon = memberCoupon;
        this.discountFromCoupon = discountAmount;
    }

    // 사용할 포인트 등록
    public void addUsePoint(int usePoint) {
        this.usePoint = usePoint;
    }

    // 배송비 설정
    public void addDeliveryFee(int deliveryFee) {
        this.deliveryFee = deliveryFee;
    }

    // 최종 금액 계산
    public void addFinalPrice(int finalPrice) {
        this.finalPrice = Math.max(finalPrice, 0);
    }

    // 연관관계 편의 메서드
    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        if (orderItem.getOrder() != this) {
            orderItem.setOrder(this);
        }
    }

    public void cancel() {
        if (this.orderState == OrderState.CANCELLED) {
            throw new BusinessException(BusinessErrorCode.ORDER_ALREADY_CANCELLED);
        }

        this.orderState = OrderState.CANCELLED;

        for (OrderItem orderItem : this.orderItems) {
            orderItem.restoreStock();
        }
    }

}