package kwh.cofshop.item.service;

import kwh.cofshop.file.domain.FileStore;
import kwh.cofshop.file.domain.UploadFile;
import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemImg;
import kwh.cofshop.item.dto.request.ItemImgRequestDto;
import kwh.cofshop.item.dto.request.ItemUpdateRequestDto;
import kwh.cofshop.item.repository.ItemImgRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class ItemImgService {

    private final FileStore fileStore;
    private final ItemImgRepository itemImgRepository;

    @Transactional
    public List<ItemImg> saveItemImages(Item item, Map<ItemImgRequestDto, MultipartFile> imgMap) throws IOException {
        List<ItemImgRequestDto> imgRequestDtoList = new ArrayList<>(imgMap.keySet());
        List<MultipartFile> fileList = new ArrayList<>(imgMap.values());

        List<UploadFile> uploadFiles = fileStore.storeFiles(fileList);
        List<ItemImg> itemImgs = new ArrayList<>();

        for (int i = 0; i < uploadFiles.size(); i++) {
            UploadFile uploadFile = uploadFiles.get(i);
            ItemImgRequestDto imgRequestDto = imgRequestDtoList.get(i);

            itemImgs.add(ItemImg.createImg(
                    uploadFile.getStoreFileName(),
                    uploadFile.getUploadFileName(),
                    "/images/" + uploadFile.getStoreFileName(),
                    imgRequestDto.getImgType(),
                    item
            ));
        }
        // DB 저장
        return itemImgRepository.saveAll(itemImgs);
    }
    @Transactional
    public void updateItemImages(Item item, ItemUpdateRequestDto dto, List<MultipartFile> imageFiles) throws IOException {
        // 기존 이미지
        List<ItemImgRequestDto> existingItemImgs = dto.getExistingItemImgs();
        Map<Long, ItemImgRequestDto> existingImageMap = existingItemImgs.stream()
                .collect(Collectors.toMap(ItemImgRequestDto::getId, img -> img));

        // 새로운 이미지
        List<ItemImgRequestDto> addItemImgs = dto.getAddItemImgs();
        if (addItemImgs != null && !addItemImgs.isEmpty() && imageFiles != null && !imageFiles.isEmpty()) {
            if (addItemImgs.size() != imageFiles.size()) {
                throw new IllegalArgumentException("추가할 이미지 정보와 실제 파일 개수가 일치하지 않습니다.");
            }

            List<UploadFile> uploadFiles = fileStore.storeFiles(imageFiles);
            List<ItemImg> newImages = new ArrayList<>();

            for (int i = 0; i < uploadFiles.size(); i++) {
                UploadFile uploadFile = uploadFiles.get(i);
                ItemImgRequestDto imgRequestDto = addItemImgs.get(i);

                // ID가 없는 경우 새 이미지 추가
                if (imgRequestDto.getId() == null) {
                    newImages.add(ItemImg.createImg(
                            uploadFile.getStoreFileName(),
                            uploadFile.getUploadFileName(),
                            "/images/" + uploadFile.getStoreFileName(),
                            imgRequestDto.getImgType(),
                            item
                    ));
                }
            }
            // 삭제할 이미지 목록
            List<Long> deleteImgIds = dto.getDeleteImgIds();

            if (deleteImgIds != null && !deleteImgIds.isEmpty()) {
                // 삭제할 이미지 조회
                List<ItemImg> deleteItems = itemImgRepository.findByItemIdAndItemImgId(item.getId(), deleteImgIds);

                // 실제 파일 삭제
                for (ItemImg img : deleteItems) {
                    if (StringUtils.hasText(img.getImgUrl())) {
                        try {
                            fileStore.deleteFile(img.getImgUrl());
                        } catch (Exception e) {
                            log.error("파일 삭제 실패: {}", img.getImgUrl(), e);
                        }
                    }
                }

                if (!deleteItems.isEmpty()) {
                    itemImgRepository.deleteAll(deleteItems);
                }
            }

            if (!newImages.isEmpty()) {
                itemImgRepository.saveAll(newImages);
            }
        }
    }
}