package kwh.cofshop.coupon.repository.outbox.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import kwh.cofshop.global.domain.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "coupon_issue_outbox",
        indexes = {
                @Index(name = "idx_coupon_outbox_status_id", columnList = "status,id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponIssueOutboxEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_issue_outbox_id")
    private Long id;

    @Column(nullable = false)
    private Long memberCouponId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long couponId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponIssueOutboxStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private int retryCount;

    @Column(length = 500)
    private String lastError;

    private LocalDateTime publishedAt;

    @Builder
    private CouponIssueOutboxEvent(Long id,
                                   Long memberCouponId,
                                   Long memberId,
                                   Long couponId,
                                   CouponIssueOutboxStatus status,
                                   String payload,
                                   int retryCount,
                                   String lastError,
                                   LocalDateTime publishedAt) {
        this.id = id;
        this.memberCouponId = memberCouponId;
        this.memberId = memberId;
        this.couponId = couponId;
        this.status = status;
        this.payload = payload;
        this.retryCount = retryCount;
        this.lastError = lastError;
        this.publishedAt = publishedAt;
    }

    public static CouponIssueOutboxEvent pending(Long memberCouponId, Long memberId, Long couponId, String payload) {
        return CouponIssueOutboxEvent.builder()
                .memberCouponId(memberCouponId)
                .memberId(memberId)
                .couponId(couponId)
                .status(CouponIssueOutboxStatus.PENDING)
                .payload(payload)
                .retryCount(0)
                .lastError(null)
                .publishedAt(null)
                .build();
    }

    public void markSent() {
        this.status = CouponIssueOutboxStatus.SENT;
        this.publishedAt = LocalDateTime.now();
        this.lastError = null;
    }

    public void markFailed(String errorMessage, int maxRetryCount) {
        this.retryCount += 1;
        this.lastError = truncate(errorMessage);
        if (this.retryCount >= maxRetryCount) {
            this.status = CouponIssueOutboxStatus.FAILED;
        }
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= 500) {
            return value;
        }
        return value.substring(0, 500);
    }
}
