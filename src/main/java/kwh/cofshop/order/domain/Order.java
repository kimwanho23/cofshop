
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

    @Column(nullable = false)
    private Integer orderYear;

    @Column(nullable = false)
    private Integer orderMonth;

    @Column(nullable = false)
    private Integer orderDay;

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
    public Order(Long id, Member member, String merchantUid, LocalDateTime orderDate, Integer orderYear, Integer orderMonth, Integer orderDay, OrderState orderState, Address address, String deliveryRequest, int deliveryFee, int totalPrice,
                 int discountFromCoupon, Integer usePoint, int finalPrice, MemberCoupon memberCoupon, List<OrderItem> orderItems, PaymentEntity payment) {
        this.id = id;
        this.member = member;
        this.merchantUid = merchantUid;
        this.orderDate = orderDate;
        this.orderYear = orderYear;
        this.orderMonth = orderMonth;
        this.orderDay = orderDay;
        this.orderState = orderState;
        this.address = address;
        this.deliveryRequest = deliveryRequest;
        this.deliveryFee = deliveryFee;
        this.totalPrice = totalPrice;
        this.discountFromCoupon = discountFromCoupon;
        this.usePoint = usePoint;
        this.finalPrice = finalPrice;
        this.memberCoupon = memberCoupon;
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
                .orderYear(LocalDateTime.now().getYear())
                .orderMonth(LocalDateTime.now().getMonthValue())
                .orderDay(LocalDateTime.now().getDayOfMonth())
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



    public static Order createOrderForce(Member member, Address address, List<OrderItem> orderItems, LocalDateTime customDate){
        Order order = Order.builder()
                .member(member)
                .address(address)
                .merchantUid(UUID.randomUUID() + "TEST")
                .orderState(OrderState.NEW)
                .orderDate(customDate)
                .orderYear(customDate.getYear())
                .orderMonth(customDate.getMonthValue())
                .orderDay(customDate.getDayOfMonth())
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