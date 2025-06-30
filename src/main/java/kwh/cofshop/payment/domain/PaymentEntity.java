package kwh.cofshop.payment.domain;

import jakarta.persistence.*;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.order.domain.Order;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String merchantUid; // 자체 생성 주문번호  ex ) "order-" + order.getId()

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "imp_uid", unique = true)
    private String impUid; // 포트원 결제 고유 ID

    @Column(name = "pg_tid", unique = true)
    private String pgTid; // PG사 거래 고유 ID

    @Column(name = "price", nullable = false)
    private Long price; // 요청 금액

    @Column(name = "paid_amount", nullable = true)
    private Long paidAmount; // 실제 결제 금액

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status; // 결제 상태

    @Column(nullable = false)
    private String buyerEmail;

    @Column(nullable = false)
    private String buyerName;

    @Column(nullable = false)
    private String buyerTel;

    @Column(name = "pg_provider", nullable = false)
    private String pgProvider;

    @Column(name = "pay_method", nullable = false)
    private String payMethod; // 결제 수단

    @Column(name = "paid_at", nullable = true)
    private LocalDateTime paidAt; // 결제 성공 시간

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt; // 결제 요청 시간

    // 연관관계

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;


    @Builder
    public PaymentEntity(Order order
                         , PaymentStatus status, String buyerEmail, String buyerName, String buyerTel, String pgProvider, String payMethod, Member member) {
        this.merchantUid = "order-" + order.getId();
        this.order = order;
        this.price = order.getFinalPrice();
        this.status = status != null ? status : PaymentStatus.READY;
        this.buyerEmail = buyerEmail;
        this.buyerName = buyerName;
        this.buyerTel = buyerTel;
        this.pgProvider = pgProvider;
        this.payMethod = payMethod;
        this.member = member;
        this.requestedAt = LocalDateTime.now();
    }

    public static PaymentEntity createPayment(Order order, String pgProvider, String payMethod) {
        Member member = order.getMember();

        return PaymentEntity.builder()
                .order(order)
                .status(PaymentStatus.READY)
                .buyerEmail(member.getEmail())
                .buyerName(member.getMemberName())
                .buyerTel(member.getTel())
                .pgProvider(pgProvider)
                .payMethod(payMethod)
                .build();
    }


    // 결제 성공 시 상태 처리, 결제 금액 및 결제일 정보 수정
    public void paymentSuccess(String impUid, String pgTid, Long paidAmount, LocalDateTime paidAt) {
        if (this.status == PaymentStatus.PAID) {
            throw new IllegalStateException("이미 결제 완료된 건입니다.");
        }
        this.impUid = impUid;
        this.pgTid = pgTid;
        this.status = PaymentStatus.PAID;
        this.paidAmount = paidAmount;
        this.paidAt = paidAt;
    }

    // 결제 상태 변경
    public void paymentStatusChange(PaymentStatus paymentStatus){
        this.status = paymentStatus;
    }

}
