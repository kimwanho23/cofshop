package kwh.cofshop.file.dto.response;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class TempFileUploadResponseDto {

    private final Long tempFileId;
    private final String uploadFileName;
    private final String imageUrl;
    private final LocalDateTime expiresAt;

    public TempFileUploadResponseDto(Long tempFileId, String uploadFileName, String imageUrl, LocalDateTime expiresAt) {
        this.tempFileId = tempFileId;
        this.uploadFileName = uploadFileName;
        this.imageUrl = imageUrl;
        this.expiresAt = expiresAt;
    }
}
