package kwh.cofshop.file.storage;

import kwh.cofshop.file.dto.response.TempFileUploadResponseDto;
import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.ForbiddenRequestException;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import kwh.cofshop.global.exception.errorcodes.ForbiddenErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TempUploadFileService {

    private final FileStore fileStore;
    private final TempUploadFileRepository tempUploadFileRepository;

    @Value("${file.temp.ttl-minutes:180}")
    private long ttlMinutes;

    @Value("${file.temp.cleanup.batch-size:100}")
    private int cleanupBatchSize;

    @Transactional
    public List<TempFileUploadResponseDto> uploadTempFiles(Long memberId, List<MultipartFile> multipartFiles) throws IOException {
        if (memberId == null || CollectionUtils.isEmpty(multipartFiles)) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
        if (multipartFiles.stream().anyMatch(MultipartFile::isEmpty)) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }

        List<UploadFile> uploadFiles = fileStore.storeFiles(multipartFiles);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(ttlMinutes);

        List<TempUploadFile> tempFiles = uploadFiles.stream()
                .map(uploadFile -> TempUploadFile.builder()
                        .ownerId(memberId)
                        .uploadFileName(uploadFile.getUploadFileName())
                        .storeFileName(uploadFile.getStoreFileName())
                        .expiresAt(expiresAt)
                        .build())
                .toList();

        try {
            List<TempUploadFile> savedFiles = tempUploadFileRepository.saveAll(tempFiles);
            return savedFiles.stream()
                    .map(file -> new TempFileUploadResponseDto(
                            file.getId(),
                            file.getUploadFileName(),
                            fileStore.getImageUrl(file.getStoreFileName()),
                            file.getExpiresAt()
                    ))
                    .toList();
        } catch (RuntimeException e) {
            cleanupStoredFiles(uploadFiles.stream().map(UploadFile::getStoreFileName).toList());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<UploadFile> resolveOwnedTempFiles(Long memberId, List<Long> tempFileIds) {
        validateTempFileIds(tempFileIds);

        LocalDateTime now = LocalDateTime.now();
        List<TempUploadFile> tempFiles = tempUploadFileRepository.findAllByIdIn(tempFileIds);
        if (tempFiles.size() != tempFileIds.size()) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }

        Map<Long, TempUploadFile> fileMap = tempFiles.stream()
                .collect(Collectors.toMap(TempUploadFile::getId, Function.identity()));

        List<UploadFile> uploadFiles = new ArrayList<>();
        for (Long tempFileId : tempFileIds) {
            TempUploadFile tempFile = fileMap.get(tempFileId);
            if (tempFile == null) {
                throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
            }
            if (!tempFile.isOwnedBy(memberId)) {
                throw new ForbiddenRequestException(ForbiddenErrorCode.MEMBER_UNAUTHORIZED);
            }
            if (tempFile.isExpired(now)) {
                throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
            }
            uploadFiles.add(new UploadFile(tempFile.getUploadFileName(), tempFile.getStoreFileName()));
        }
        return uploadFiles;
    }

    @Transactional
    public void consumeOwnedTempFiles(Long memberId, List<Long> tempFileIds) {
        validateTempFileIds(tempFileIds);
        List<TempUploadFile> tempFiles = tempUploadFileRepository.findAllByIdIn(tempFileIds);
        if (tempFiles.size() != tempFileIds.size()) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
        if (tempFiles.stream().anyMatch(file -> !file.isOwnedBy(memberId))) {
            throw new ForbiddenRequestException(ForbiddenErrorCode.MEMBER_UNAUTHORIZED);
        }
        tempUploadFileRepository.deleteAllInBatch(tempFiles);
    }

    @Transactional
    public void deleteOwnedTempFile(Long memberId, Long tempFileId) {
        TempUploadFile tempFile = tempUploadFileRepository.findByIdAndOwnerId(tempFileId, memberId)
                .orElseThrow(() -> new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE));

        fileStore.deleteFile(tempFile.getStoreFileName());
        tempUploadFileRepository.delete(tempFile);
    }

    @Scheduled(fixedDelayString = "${file.temp.cleanup.fixed-delay-ms:600000}")
    @Transactional
    public void cleanupExpiredTempFiles() {
        LocalDateTime now = LocalDateTime.now();
        List<TempUploadFile> expiredFiles = tempUploadFileRepository.findByExpiresAtBeforeOrderByExpiresAtAsc(
                now,
                PageRequest.of(0, cleanupBatchSize)
        );

        if (expiredFiles.isEmpty()) {
            return;
        }

        List<TempUploadFile> deletedTargets = new ArrayList<>();
        for (TempUploadFile expiredFile : expiredFiles) {
            try {
                fileStore.deleteFile(expiredFile.getStoreFileName());
                deletedTargets.add(expiredFile);
            } catch (RuntimeException e) {
                log.warn("만료 임시파일 삭제 실패 - storeFileName={}", expiredFile.getStoreFileName(), e);
            }
        }
        if (!deletedTargets.isEmpty()) {
            tempUploadFileRepository.deleteAllInBatch(deletedTargets);
        }
    }

    private void validateTempFileIds(List<Long> tempFileIds) {
        if (CollectionUtils.isEmpty(tempFileIds) || tempFileIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
        if (new LinkedHashSet<>(tempFileIds).size() != tempFileIds.size()) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
    }

    private void cleanupStoredFiles(List<String> storeFileNames) {
        for (String storeFileName : storeFileNames) {
            try {
                fileStore.deleteFile(storeFileName);
            } catch (RuntimeException e) {
                log.warn("보상 삭제 실패 - storeFileName={}", storeFileName, e);
            }
        }
    }
}
