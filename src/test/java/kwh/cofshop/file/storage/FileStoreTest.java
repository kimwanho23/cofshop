package kwh.cofshop.file.storage;

import kwh.cofshop.global.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

class FileStoreTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0};
    private static final byte[] GIF = "GIF89a-data".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP = "RIFF0000WEBPVP8".getBytes(StandardCharsets.US_ASCII);

    @TempDir
    Path tempDir;

    private FileStore fileStore;

    @BeforeEach
    void setUp() {
        fileStore = new FileStore(new LocalFileStorage(tempDir.toString()));
    }

    @Test
    @DisplayName("이미지 파일 저장 성공")
    void storeFile_success() throws IOException {
        MockMultipartFile file = new MockMultipartFile("images", "test.jpg", "image/jpeg", JPEG);

        UploadFile result = fileStore.storeFile(file);

        assertThat(result).isNotNull();
        assertThat(result.getStoreFileName()).endsWith(".jpg");
    }

    @Test
    @DisplayName("허용된 형식은 내용이 맞으면 모두 저장")
    void storeFile_allowedFormats() throws IOException {
        fileStore.storeFile(new MockMultipartFile("images", "a.jpeg", "image/jpeg", JPEG));
        fileStore.storeFile(new MockMultipartFile("images", "a.png", "image/png", PNG));
        fileStore.storeFile(new MockMultipartFile("images", "a.gif", "image/gif", GIF));
        fileStore.storeFile(new MockMultipartFile("images", "a.webp", "image/webp", WEBP));

        assertThat(storedFileCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("이미지 확장자 없는 파일은 실패")
    void storeFile_withoutExtension() {
        MockMultipartFile file = new MockMultipartFile("images", "test", "image/jpeg", JPEG);

        assertThatThrownBy(() -> fileStore.storeFile(file))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("비이미지 content-type 파일은 실패")
    void storeFile_nonImageContentType() {
        MockMultipartFile file = new MockMultipartFile("images", "test.jpg", "text/plain", JPEG);

        assertThatThrownBy(() -> fileStore.storeFile(file))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("허용되지 않은 확장자는 실패")
    void storeFile_notAllowedExtension() {
        MockMultipartFile file = new MockMultipartFile("images", "test.svg", "image/svg+xml", "<svg/>".getBytes());

        assertThatThrownBy(() -> fileStore.storeFile(file))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("확장자와 content-type을 속인 파일은 실패")
    void storeFile_forgedContent() throws IOException {
        MockMultipartFile html = new MockMultipartFile("images", "test.png", "image/png", "<script>x</script>".getBytes());
        MockMultipartFile otherFormat = new MockMultipartFile("images", "test.png", "image/png", JPEG);
        MockMultipartFile tooShort = new MockMultipartFile("images", "test.webp", "image/webp", "RIFF".getBytes());

        assertThatThrownBy(() -> fileStore.storeFile(html)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> fileStore.storeFile(otherFormat)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> fileStore.storeFile(tooShort)).isInstanceOf(BadRequestException.class);
        assertThat(storedFileCount()).isZero();
    }

    @Test
    @DisplayName("파일명이 너무 길면 실패")
    void storeFile_tooLongFileName() {
        MockMultipartFile file = new MockMultipartFile("images", "a".repeat(252) + ".jpg", "image/jpeg", JPEG);

        assertThatThrownBy(() -> fileStore.storeFile(file))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("여러 파일 중 하나가 잘못되면 아무것도 저장하지 않음")
    void storeFiles_invalidOne_storesNothing() throws IOException {
        MockMultipartFile valid = new MockMultipartFile("images", "a.jpg", "image/jpeg", JPEG);
        MockMultipartFile invalid = new MockMultipartFile("images", "b.jpg", "image/jpeg", "data".getBytes());

        assertThatThrownBy(() -> fileStore.storeFiles(List.of(valid, invalid)))
                .isInstanceOf(BadRequestException.class);

        assertThat(storedFileCount()).isZero();
    }

    @Test
    @DisplayName("저장 중 실패하면 앞서 저장한 파일을 지움")
    void storeFiles_writeFailure_removesStored() throws IOException {
        MockMultipartFile valid = new MockMultipartFile("images", "a.jpg", "image/jpeg", JPEG);
        MockMultipartFile failing = spy(new MockMultipartFile("images", "b.jpg", "image/jpeg", JPEG));
        doThrow(new IOException("disk full")).when(failing).transferTo(any(java.io.File.class));

        assertThatThrownBy(() -> fileStore.storeFiles(List.of(valid, failing)))
                .isInstanceOf(IOException.class);

        assertThat(storedFileCount()).isZero();
    }

    private long storedFileCount() throws IOException {
        try (Stream<Path> files = Files.list(tempDir)) {
            return files.count();
        }
    }
}
