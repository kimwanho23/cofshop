package kwh.cofshop.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 탈퇴 회원의 거래기록 중 개인정보. 분쟁·법정 보존 목적으로만 두고 만료되면 삭제한다
@Entity
@Table(name = "order_personal_archive")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderPersonalArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_personal_archive_id")
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    private String buyerEmail;

    @Column(length = 50)
    private String buyerName;

    private String buyerTel;

    private String city;

    private String street;

    private String zipCode;

    private String deliveryRequest;

    @Column(nullable = false)
    private LocalDateTime archivedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public static OrderPersonalArchive of(Order order, String buyerEmail, String buyerName, String buyerTel,
                                          LocalDateTime archivedAt, LocalDateTime expiresAt) {
        OrderPersonalArchive archive = new OrderPersonalArchive();
        archive.orderId = order.getId();
        archive.buyerEmail = buyerEmail;
        archive.buyerName = buyerName;
        archive.buyerTel = buyerTel;
        Address address = order.getAddress();
        if (address != null) {
            archive.city = address.getCity();
            archive.street = address.getStreet();
            archive.zipCode = address.getZipCode();
        }
        archive.deliveryRequest = order.getDeliveryRequest();
        archive.archivedAt = archivedAt;
        archive.expiresAt = expiresAt;
        return archive;
    }
}
