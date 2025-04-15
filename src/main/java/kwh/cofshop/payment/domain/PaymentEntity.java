package kwh.cofshop.payment.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    private PaymentStatus status; // 결제 상태

    private String paymentUid; // 결제 번호

    private Long price; // 가격

    @Builder
    public PaymentEntity(Long id, PaymentStatus status, String paymentUid, Long price) {
        this.id = id;
        this.status = status;
        this.paymentUid = paymentUid;
        this.price = price;
    }

    public void changePaymentBySuccess(PaymentStatus status, String paymentUid) {
        this.status = status;
        this.paymentUid = paymentUid;
    }
}
