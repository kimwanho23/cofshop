package kwh.cofshop.file.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@ConditionalOnProperty(name = "file.storage", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    private final String fileDir;

    public LocalFileStorage(@Value("${file.dir:./uploads}") String fileDir) {
        this.fileDir = fileDir;
    }

    @Override
    public void store(String storeFileName, MultipartFile file, String contentType) throws IOException {
        Path targetPath = resolve(storeFileName);
        Path parent = targetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        file.transferTo(targetPath.toFile());
    }

    @Override
    public void delete(String storeFileName) {
        Path path = resolve(storeFileName);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("파일 삭제 실패: " + path, e);
        }
    }

    @Override
    public String getUrl(String storeFileName) {
        return "/images/" + storeFileName;
    }

    // 상대 경로는 transferTo가 Tomcat 임시 폴더 기준으로 해석하므로 절대 경로로 변환
    private Path resolve(String storeFileName) {
        return Paths.get(fileDir, storeFileName).toAbsolutePath().normalize();
    }
}
