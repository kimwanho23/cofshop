package kwh.cofshop.file.storage;

import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class FileStore {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    @Value("${file.dir}")
    private String fileDir;

    public String getFullPath(String filename) {
        return Paths.get(fileDir, filename).normalize().toString();
    }

    public String getImageUrl(String storeFileName) {
        String normalizedFileName = sanitizeFileName(storeFileName);
        if (!StringUtils.hasText(normalizedFileName)) {
            return null;
        }
        return "/images/" + normalizedFileName;
    }

    // 파일 다수 저장
    public List<UploadFile> storeFiles(List<MultipartFile> multipartFiles) throws IOException {
        List<UploadFile> storeFileResult = new ArrayList<>();
        for (MultipartFile multipartFile : multipartFiles) {
            if (!multipartFile.isEmpty()) {
                storeFileResult.add(storeFile(multipartFile));
            }
        }
        return storeFileResult;
    }

    // 파일 개별 저장
    public UploadFile storeFile(MultipartFile multipartFile) throws IOException {
        if (multipartFile.isEmpty()) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
        String originalFilename = multipartFile.getOriginalFilename();
        validateImageFile(multipartFile, originalFilename);
        String storeFileName = createStoreFileName(originalFilename);
        Path targetPath = Paths.get(getFullPath(storeFileName));
        Path parent = targetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        multipartFile.transferTo(targetPath.toFile());
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

        Path path = Paths.get(getFullPath(normalizedFileName));
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("파일 삭제 실패: " + path, e);
        }
    }

    private String sanitizeFileName(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.replace("\\", "/");
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        Path path = Paths.get(normalized).getFileName();
        return path == null ? null : path.toString();
    }

    private void validateImageFile(MultipartFile multipartFile, String originalFilename) {
        String ext = extractExt(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
        String contentType = multipartFile.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
    }
}
