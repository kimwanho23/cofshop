package kwh.cofshop.file.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TempUploadFileRepository extends JpaRepository<TempUploadFile, Long> {

    List<TempUploadFile> findAllByIdIn(List<Long> ids);

    Optional<TempUploadFile> findByIdAndOwnerId(Long id, Long ownerId);

    List<TempUploadFile> findByExpiresAtBeforeOrderByExpiresAtAsc(LocalDateTime now, Pageable pageable);
}
