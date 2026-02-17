package kwh.cofshop.item.service;

import kwh.cofshop.file.storage.FileStore;
import kwh.cofshop.file.storage.TempUploadFileService;
import kwh.cofshop.file.storage.UploadFile;
import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemImg;
import kwh.cofshop.item.dto.request.ItemImgRequestDto;
import kwh.cofshop.item.dto.request.ItemUpdateRequestDto;
import kwh.cofshop.item.repository.ItemImgRepository;
import kwh.cofshop.item.vo.ItemImgUploadVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class ItemImgService {

    private final FileStore fileStore;
    private final TempUploadFileService tempUploadFileService;
    private final ItemImgRepository itemImgRepository;

    @Transactional
    public List<ItemImg> saveItemImages(Item item, List<ItemImgUploadVO> uploadVOList) throws IOException {
        List<ItemImgRequestDto> imgRequestDtos = uploadVOList.stream()
                .map(ItemImgUploadVO::getImgRequestDto)
                .collect(Collectors.toList());
        List<MultipartFile> fileList = uploadVOList.stream()
                .map(ItemImgUploadVO::getMultipartFile)
                .collect(Collectors.toList());
        return saveItemImagesFromMultipart(item, imgRequestDtos, fileList);
    }

    @Transactional
    public List<ItemImg> saveItemImagesFromMultipart(Item item, List<ItemImgRequestDto> imgRequestDtos, List<MultipartFile> imageFiles) throws IOException {
        if (imgRequestDtos == null || imageFiles == null || imgRequestDtos.size() != imageFiles.size()) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
        List<UploadFile> uploadFiles = fileStore.storeFiles(imageFiles);
        try {
            return saveItemImagesFromStoredFiles(item, imgRequestDtos, uploadFiles);
        } catch (RuntimeException e) {
            cleanupUploadFiles(uploadFiles);
            throw e;
        }
    }

    @Transactional
    public List<ItemImg> saveItemImagesFromStoredFiles(Item item, List<ItemImgRequestDto> imgRequestDtos, List<UploadFile> uploadFiles) {
        if (imgRequestDtos == null || uploadFiles == null || imgRequestDtos.size() != uploadFiles.size()) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }

        List<ItemImg> itemImgs = new ArrayList<>();
        for (int i = 0; i < uploadFiles.size(); i++) {
            UploadFile uploadFile = uploadFiles.get(i);
            ItemImgRequestDto imgRequestDto = imgRequestDtos.get(i);

            itemImgs.add(ItemImg.createImg(
                    uploadFile.getStoreFileName(),
                    uploadFile.getUploadFileName(),
                    fileStore.getImageUrl(uploadFile.getStoreFileName()),
                    imgRequestDto.getImgType(),
                    item
            ));
        }
        return itemImgRepository.saveAll(itemImgs);
    }


    // 수정 시 삭제할 이미지 목록
    public void deleteItemImages(Item item, List<Long> deleteImgIds) {
        if (deleteImgIds != null && !deleteImgIds.isEmpty()) {
            // 삭제할 이미지 조회
            List<ItemImg> deleteItems = itemImgRepository.findByItemIdAndItemImgId(item.getId(), deleteImgIds);

            // 실제 파일 삭제
            for (ItemImg img : deleteItems) {
                if (StringUtils.hasText(img.getImgName())) {
                    try {
                        fileStore.deleteFile(img.getImgName());
                    } catch (Exception e) {
                        log.error("파일 삭제 실패: {}", img.getImgName(), e);
                    }
                }
            }

            // DB에서 이미지 정보 삭제
            if (!deleteItems.isEmpty()) {
                itemImgRepository.deleteAll(deleteItems);
            }
        }
    }


    // 수정 시 새로운 이미지 등록
    public List<ItemImg> addItemImages(Item item, List<ItemImgRequestDto> addItemImgs, List<MultipartFile> imageFiles, Long memberId) throws IOException {
        if (addItemImgs == null || addItemImgs.isEmpty()) {
            return Collections.emptyList();
        }

        List<ItemImgRequestDto> newImageRequests = addItemImgs.stream()
                .filter(imgRequestDto -> imgRequestDto.getId() == null)
                .collect(Collectors.toList());
        if (newImageRequests.isEmpty()) {
            return Collections.emptyList();
        }

        boolean hasDirectFiles = imageFiles != null && !imageFiles.isEmpty();
        boolean containsTempFileIds = newImageRequests.stream().anyMatch(imgRequestDto -> imgRequestDto.getTempFileId() != null);
        boolean hasTempFileIds = newImageRequests.stream().allMatch(imgRequestDto -> imgRequestDto.getTempFileId() != null);

        if (hasDirectFiles && containsTempFileIds) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
        if (!hasDirectFiles && !hasTempFileIds) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }

        if (hasDirectFiles) {
            if (newImageRequests.size() != imageFiles.size()) {
                throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
            }
            return saveItemImagesFromMultipart(item, newImageRequests, imageFiles);
        }

        List<Long> tempFileIds = newImageRequests.stream()
                .map(ItemImgRequestDto::getTempFileId)
                .collect(Collectors.toList());
        List<UploadFile> tempUploadFiles = tempUploadFileService.resolveOwnedTempFiles(memberId, tempFileIds);
        List<ItemImg> savedImages = saveItemImagesFromStoredFiles(item, newImageRequests, tempUploadFiles);
        tempUploadFileService.consumeOwnedTempFiles(memberId, tempFileIds);
        return savedImages;
    }

    @Transactional
    public List<ItemImg> updateItemImages(Item item, ItemUpdateRequestDto dto, List<MultipartFile> imageFiles, Long memberId) throws IOException {
        deleteItemImages(item, dto.getDeleteImgIds()); // 이미지 삭제
        return addItemImages(item, dto.getAddItemImgs(), imageFiles, memberId); // 이미지 추가
    }

    @Transactional
    public List<ItemImg> updateItemImages(Item item, ItemUpdateRequestDto dto, List<MultipartFile> imageFiles) throws IOException {
        return updateItemImages(item, dto, imageFiles, null);
    }

    public void deleteStoredFiles(List<ItemImg> itemImgs) {
        if (itemImgs == null || itemImgs.isEmpty()) {
            return;
        }
        for (ItemImg itemImg : itemImgs) {
            if (!StringUtils.hasText(itemImg.getImgName())) {
                continue;
            }
            try {
                fileStore.deleteFile(itemImg.getImgName());
            } catch (RuntimeException e) {
                log.warn("파일 보상 삭제 실패: {}", itemImg.getImgName(), e);
            }
        }
    }

    private void cleanupUploadFiles(List<UploadFile> uploadFiles) {
        for (UploadFile uploadFile : uploadFiles) {
            try {
                fileStore.deleteFile(uploadFile.getStoreFileName());
            } catch (RuntimeException e) {
                log.warn("업로드 실패 보상 삭제 실패: {}", uploadFile.getStoreFileName(), e);
            }
        }
    }
}
