package kwh.cofshop.order.domain;

import jakarta.persistence.*;
import kwh.cofshop.global.domain.BaseTimeEntity;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.member.domain.Member;
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
@Table(name = "orders")
public class Order extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id", nullable = false, updatable = false)
    private Long id;

    // 주문자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // 주문 정보
    @Column(nullable = false, unique = true)
    private String merchantUid;

    // 주문 날짜
    @Column(nullable = false)
    private LocalDateTime orderDate;

    // 주문년도
    @Column(nullable = false)
    private Integer orderYear;

    // 주문월
    @Column(nullable = false)
    private Integer orderMonth;

    // 주문일
    @Column(nullable = false)
    private Integer orderDay;

    // 배송 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "order_state", nullable = false)
    private OrderState orderState;

    // 주소
    @Embedded
    private Address address;

    // 주문 요청 사항
    private String deliveryRequest;

    // 배송비
    private int deliveryFee;

    // 상품 총액
    @Column(nullable = false)
    private Long totalPrice;

    // 쿠폰 할인 금액
    private Long discountFromCoupon;

    // 포인트 사용량
    private Integer usePoint;

    // 최종 금액
    private Long finalPrice;

    // 쿠폰
    @Column(name = "member_coupon_id")
    private Long memberCouponId;

    // 연관 관계
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Builder
    public Order(Long id, Member member, String merchantUid, LocalDateTime orderDate, Integer orderYear, Integer orderMonth, Integer orderDay, OrderState orderState, Address address, String deliveryRequest, int deliveryFee, Long totalPrice,
                 Long discountFromCoupon, Integer usePoint, Long finalPrice, Long memberCouponId, List<OrderItem> orderItems) {
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
        this.memberCouponId = memberCouponId;
        this.orderItems = orderItems;
    }

    // 주문 생성
    public static Order createOrder(Member member, Address address, String deliveryRequest, List<OrderItem> orderItems) {
        LocalDateTime now = LocalDateTime.now();
        Order order = Order.builder()
                .member(member)
                .address(address)
                .deliveryRequest(deliveryRequest)
                .merchantUid("cofshop" + UUID.randomUUID())
                .orderState(OrderState.WAITING_FOR_PAY)
                .orderDate(now)
                .orderYear(now.getYear())
                .orderMonth(now.getMonthValue())
                .orderDay(now.getDayOfMonth())
                .orderItems(new ArrayList<>())
                .totalPrice(orderItems.stream()
                        .mapToLong(OrderItem::getTotalPrice)
                        .sum()
                )
                .build();
        for (OrderItem orderItem : orderItems) {
            order.addOrderItem(orderItem);
        }
        return order;
    }

    public void pay() {
        if (this.getOrderState() != OrderState.WAITING_FOR_PAY) {
            throw new BusinessException(BusinessErrorCode.PAYMENT_FAIL);
        }
        this.changeOrderState(OrderState.PAYMENT_PENDING); // Order 테이블에 결제 저장
    }


    // 주문 상태 변경
    public void changeOrderState(OrderState orderState) {
        this.orderState = orderState;
    }


    // 사용할 쿠폰 등록
    public void addUseCoupon(Long memberCouponId, Long discountAmount) {
        this.memberCouponId = memberCouponId;
        this.discountFromCoupon = discountAmount;
    }

    // 사용할 포인트 등록
    public void addUsePoint(int usePoint) {
        this.usePoint = usePoint;
    }

    // 최종 금액 환산
    public void finalizePrice(long priceAfterCoupon, int usePoint, int deliveryFee) {
        this.deliveryFee = deliveryFee;
        this.finalPrice = Math.max(priceAfterCoupon - usePoint + deliveryFee, 0);
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
        if (this.orderState == OrderState.COMPLETED) {
            throw new BusinessException(BusinessErrorCode.ORDER_ALREADY_COMPLETED);
        }
        if (this.orderState != OrderState.WAITING_FOR_PAY
                && this.orderState != OrderState.PAYMENT_PENDING
                && this.orderState != OrderState.REFUND_PENDING
                && this.orderState != OrderState.PAID) {
            throw new BusinessException(BusinessErrorCode.ORDER_CANNOT_CANCEL);
        }
        this.orderState = OrderState.CANCELLED;
    }

}
