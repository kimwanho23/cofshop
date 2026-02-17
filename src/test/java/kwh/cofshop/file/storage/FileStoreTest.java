package kwh.cofshop.file.storage;

import kwh.cofshop.global.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStoreTest {

    @TempDir
    Path tempDir;

    private FileStore fileStore;

    @BeforeEach
    void setUp() {
        fileStore = new FileStore();
        ReflectionTestUtils.setField(fileStore, "fileDir", tempDir.toString());
    }

    @Test
    @DisplayName("이미지 파일 저장 성공")
    void storeFile_success() throws IOException {
        MockMultipartFile file = new MockMultipartFile("images", "test.jpg", "image/jpeg", "data".getBytes());

        UploadFile result = fileStore.storeFile(file);

        assertThat(result).isNotNull();
        assertThat(result.getStoreFileName()).endsWith(".jpg");
    }

    @Test
    @DisplayName("이미지 확장자 없는 파일은 실패")
    void storeFile_withoutExtension() {
        MockMultipartFile file = new MockMultipartFile("images", "test", "image/jpeg", "data".getBytes());

        assertThatThrownBy(() -> fileStore.storeFile(file))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("비이미지 content-type 파일은 실패")
    void storeFile_nonImageContentType() {
        MockMultipartFile file = new MockMultipartFile("images", "test.jpg", "text/plain", "data".getBytes());

        assertThatThrownBy(() -> fileStore.storeFile(file))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("허용되지 않은 확장자는 실패")
    void storeFile_notAllowedExtension() {
        MockMultipartFile file = new MockMultipartFile("images", "test.svg", "image/svg+xml", "data".getBytes());

        assertThatThrownBy(() -> fileStore.storeFile(file))
                .isInstanceOf(BadRequestException.class);
    }
}
