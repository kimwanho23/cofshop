package kwh.cofshop.file.storage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "temp_upload_file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TempUploadFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "temp_upload_file_id")
    private Long id;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 255)
    private String uploadFileName;

    @Column(nullable = false, length = 255, unique = true)
    private String storeFileName;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder
    public TempUploadFile(Long ownerId, String uploadFileName, String storeFileName, LocalDateTime expiresAt) {
        this.ownerId = ownerId;
        this.uploadFileName = uploadFileName;
        this.storeFileName = storeFileName;
        this.expiresAt = expiresAt;
    }

    public boolean isOwnedBy(Long memberId) {
        return ownerId != null && ownerId.equals(memberId);
    }

    public boolean isExpired(LocalDateTime now) {
        return expiresAt != null && expiresAt.isBefore(now);
    }
}
