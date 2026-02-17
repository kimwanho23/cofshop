package kwh.cofshop.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import kwh.cofshop.file.dto.response.TempFileUploadResponseDto;
import kwh.cofshop.file.storage.TempUploadFileService;
import kwh.cofshop.global.annotation.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class FileController {

    private final TempUploadFileService tempUploadFileService;

    @Operation(summary = "임시 이미지 업로드", description = "게시글 작성 전 미리보기용 임시 이미지를 업로드합니다.")
    @PostMapping("/temp")
    public List<TempFileUploadResponseDto> uploadTempImages(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @RequestPart("images") List<MultipartFile> images
    ) throws IOException {
        return tempUploadFileService.uploadTempFiles(memberId, images);
    }

    @Operation(summary = "임시 이미지 삭제", description = "게시글 작성 취소 시 임시 이미지를 즉시 삭제합니다.")
    @DeleteMapping("/temp/{tempFileId}")
    public ResponseEntity<Void> deleteTempImage(
            @Parameter(hidden = true) @LoginMember Long memberId,
            @PathVariable Long tempFileId
    ) {
        tempUploadFileService.deleteOwnedTempFile(memberId, tempFileId);
        return ResponseEntity.noContent().build();
    }
}
