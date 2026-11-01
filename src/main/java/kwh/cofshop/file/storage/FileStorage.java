package kwh.cofshop.file.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

// 파일을 실제로 보관하는 곳. 서버가 여러 대면 공유 스토리지(s3)를 사용
public interface FileStorage {

    void store(String storeFileName, MultipartFile file, String contentType) throws IOException;

    void delete(String storeFileName);

    String getUrl(String storeFileName);
}
