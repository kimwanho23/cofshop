package kwh.cofshop.file.storage;

import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileStore {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    // 클라이언트가 보낸 값 대신 검증한 확장자로 저장
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "gif", "image/gif"
    );
    private static final int MAX_FILE_NAME_LENGTH = 255; // temp_upload_file.upload_file_name 길이
    private static final int SIGNATURE_LENGTH = 12;

    private final FileStorage fileStorage;

    public String getImageUrl(String storeFileName) {
        String normalizedFileName = sanitizeFileName(storeFileName);
        if (!StringUtils.hasText(normalizedFileName)) {
            return null;
        }
        return fileStorage.getUrl(normalizedFileName);
    }

    // 파일 다수 저장
    public List<UploadFile> storeFiles(List<MultipartFile> multipartFiles) throws IOException {
        List<MultipartFile> targets = multipartFiles.stream()
                .filter(multipartFile -> !multipartFile.isEmpty())
                .toList();
        // 일부만 저장된 채 실패하면 DB에 없는 파일이 공개 경로에 남으므로 먼저 전부 검사
        for (MultipartFile multipartFile : targets) {
            validateImageFile(multipartFile, multipartFile.getOriginalFilename());
        }

        List<UploadFile> storeFileResult = new ArrayList<>();
        try {
            for (MultipartFile multipartFile : targets) {
                storeFileResult.add(storeFile(multipartFile));
            }
        } catch (IOException | RuntimeException e) {
            deleteQuietly(storeFileResult);
            throw e;
        }
        return storeFileResult;
    }

    private void deleteQuietly(List<UploadFile> uploadFiles) {
        for (UploadFile uploadFile : uploadFiles) {
            try {
                deleteFile(uploadFile.getStoreFileName());
            } catch (RuntimeException e) {
                log.warn("업로드 실패 보상 삭제 실패: {}", uploadFile.getStoreFileName(), e);
            }
        }
    }

    // 파일 개별 저장
    public UploadFile storeFile(MultipartFile multipartFile) throws IOException {
        if (multipartFile.isEmpty()) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
        String originalFilename = multipartFile.getOriginalFilename();
        validateImageFile(multipartFile, originalFilename);
        String storeFileName = createStoreFileName(originalFilename);
        fileStorage.store(storeFileName, multipartFile, CONTENT_TYPES.get(extractExt(originalFilename)));
        return new UploadFile(originalFilename, storeFileName);
    }

    // 파일명 지정
    private String createStoreFileName(String originalFilename) {
        String ext = extractExt(originalFilename);
        String uuid = UUID.randomUUID().toString();
        return uuid + "." + ext;
    }

    // 확장자 추출
    private String extractExt(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
        int pos = originalFilename.lastIndexOf(".");
        if (pos < 0 || pos == originalFilename.length() - 1) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
        return originalFilename.substring(pos + 1).toLowerCase(Locale.ROOT);
    }

    // 파일 삭제
    public void deleteFile(String storeFileName) {
        String normalizedFileName = sanitizeFileName(storeFileName);
        if (!StringUtils.hasText(normalizedFileName)) {
            return;
        }

        fileStorage.delete(normalizedFileName);
    }

    private String sanitizeFileName(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.replace("\\", "/");
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        java.nio.file.Path path = Paths.get(normalized).getFileName();
        return path == null ? null : path.toString();
    }

    private void validateImageFile(MultipartFile multipartFile, String originalFilename) throws IOException {
        String ext = extractExt(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(ext) || originalFilename.length() > MAX_FILE_NAME_LENGTH) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
        String contentType = multipartFile.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
        if (!matchesSignature(multipartFile, ext)) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
    }

    // 확장자와 Content-Type은 클라이언트가 정하는 값이므로 실제 내용의 시그니처를 확인
    private boolean matchesSignature(MultipartFile multipartFile, String ext) throws IOException {
        byte[] header;
        try (InputStream inputStream = multipartFile.getInputStream()) {
            header = inputStream.readNBytes(SIGNATURE_LENGTH);
        }
        return switch (ext) {
            case "jpg", "jpeg" -> startsWith(header, 0, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case "png" -> startsWith(header, 0, new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
            case "gif" -> startsWith(header, 0, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                    || startsWith(header, 0, "GIF89a".getBytes(StandardCharsets.US_ASCII));
            case "webp" -> startsWith(header, 0, "RIFF".getBytes(StandardCharsets.US_ASCII))
                    && startsWith(header, 8, "WEBP".getBytes(StandardCharsets.US_ASCII));
            default -> false;
        };
    }

    private boolean startsWith(byte[] header, int offset, byte[] signature) {
        if (header.length < offset + signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (header[offset + i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
