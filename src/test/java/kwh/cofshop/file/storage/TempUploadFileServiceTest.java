package kwh.cofshop.file.storage;

import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.ForbiddenRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TempUploadFileServiceTest {

    @Mock
    private FileStore fileStore;

    @Mock
    private TempUploadFileRepository tempUploadFileRepository;

    @InjectMocks
    private TempUploadFileService tempUploadFileService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tempUploadFileService, "ttlMinutes", 180L);
        ReflectionTestUtils.setField(tempUploadFileService, "cleanupBatchSize", 100);
    }

    @Test
    @DisplayName("임시 파일 업로드: 성공")
    void uploadTempFiles_success() throws IOException {
        MockMultipartFile file = new MockMultipartFile("images", "temp.jpg", "image/jpeg", "data".getBytes());
        when(fileStore.storeFiles(List.of(file))).thenReturn(List.of(new UploadFile("temp.jpg", "temp-store.jpg")));

        TempUploadFile entity = TempUploadFile.builder()
                .ownerId(1L)
                .uploadFileName("temp.jpg")
                .storeFileName("temp-store.jpg")
                .expiresAt(LocalDateTime.now().plusHours(3))
                .build();
        ReflectionTestUtils.setField(entity, "id", 10L);

        when(tempUploadFileRepository.saveAll(anyList())).thenReturn(List.of(entity));
        when(fileStore.getImageUrl("temp-store.jpg")).thenReturn("/images/temp-store.jpg");

        var result = tempUploadFileService.uploadTempFiles(1L, List.of(file));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTempFileId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("임시 파일 조회: 중복 ID 요청은 실패")
    void resolveOwnedTempFiles_duplicateIds() {
        assertThatThrownBy(() -> tempUploadFileService.resolveOwnedTempFiles(1L, List.of(1L, 1L)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("임시 파일 조회: 다른 사용자 소유 파일은 실패")
    void resolveOwnedTempFiles_forbidden() {
        TempUploadFile entity = TempUploadFile.builder()
                .ownerId(2L)
                .uploadFileName("temp.jpg")
                .storeFileName("temp-store.jpg")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        ReflectionTestUtils.setField(entity, "id", 10L);

        when(tempUploadFileRepository.findAllByIdIn(List.of(10L))).thenReturn(List.of(entity));

        assertThatThrownBy(() -> tempUploadFileService.resolveOwnedTempFiles(1L, List.of(10L)))
                .isInstanceOf(ForbiddenRequestException.class);
    }

    @Test
    @DisplayName("만료 임시 파일 정리")
    void cleanupExpiredTempFiles() {
        TempUploadFile expired = TempUploadFile.builder()
                .ownerId(1L)
                .uploadFileName("temp.jpg")
                .storeFileName("temp-store.jpg")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(tempUploadFileRepository.findByExpiresAtBeforeOrderByExpiresAtAsc(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(expired));

        tempUploadFileService.cleanupExpiredTempFiles();

        verify(fileStore).deleteFile("temp-store.jpg");
        verify(tempUploadFileRepository).deleteAllInBatch(List.of(expired));
    }

    @Test
    @DisplayName("만료 임시 파일 정리: 파일 삭제 실패 건은 메타 삭제하지 않음")
    void cleanupExpiredTempFiles_skipFailedDelete() {
        TempUploadFile expired = TempUploadFile.builder()
                .ownerId(1L)
                .uploadFileName("temp.jpg")
                .storeFileName("temp-store.jpg")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(tempUploadFileRepository.findByExpiresAtBeforeOrderByExpiresAtAsc(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(expired));
        org.mockito.Mockito.doThrow(new RuntimeException("delete failed")).when(fileStore).deleteFile("temp-store.jpg");

        tempUploadFileService.cleanupExpiredTempFiles();

        verify(tempUploadFileRepository, org.mockito.Mockito.never()).deleteAllInBatch(anyList());
    }
}
