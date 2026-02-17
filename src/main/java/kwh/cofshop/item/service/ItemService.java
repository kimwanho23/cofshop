package kwh.cofshop.item.service;

import kwh.cofshop.file.storage.TempUploadFileService;
import kwh.cofshop.file.storage.UploadFile;
import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.ForbiddenRequestException;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.global.exception.errorcodes.ForbiddenErrorCode;
import kwh.cofshop.item.api.PopularItemPort;
import kwh.cofshop.item.domain.*;
import kwh.cofshop.item.dto.request.ItemImgRequestDto;
import kwh.cofshop.item.dto.request.ItemRequestDto;
import kwh.cofshop.item.dto.request.ItemSearchRequestDto;
import kwh.cofshop.item.dto.request.ItemUpdateRequestDto;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import kwh.cofshop.item.dto.response.ItemSearchResponseDto;
import kwh.cofshop.item.mapper.ItemMapper;
import kwh.cofshop.item.repository.*;
import kwh.cofshop.item.vo.ItemImgUploadVO;
import kwh.cofshop.member.api.MemberReadPort;
import kwh.cofshop.member.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemService { // 통합 Item 서비스
    private final ItemRepository itemRepository;

    // 매퍼 클래스
    private final ItemMapper itemMapper;

    // 연관관계 서비스
    private final ItemImgService itemImgService; // 이미지 서비스
    private final ItemOptionService itemOptionService; // 옵션 서비스
    private final ItemCategoryService itemCategoryService;

    private final MemberReadPort memberReadPort;
    private final TempUploadFileService tempUploadFileService;
    private final CategoryRepository categoryRepository;
    private final ItemCategoryRepository itemCategoryRepository;
    private final ItemOptionRepository itemOptionRepository;
    private final ItemImgRepository itemImgRepository;
    private final PopularItemPort popularItemPort;

    @Transactional
    public ItemResponseDto saveItem(ItemRequestDto itemRequestDto, Long id, List<MultipartFile> images) throws IOException {
        // 상품 등록자
        Member member = memberReadPort.getById(id);

        List<ItemImgRequestDto> imgRequestDtoList = itemRequestDto.getItemImages();
        if (imgRequestDtoList == null || imgRequestDtoList.isEmpty()) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
        List<MultipartFile> safeImages = images == null ? Collections.emptyList() : images;
        boolean hasDirectImages = !safeImages.isEmpty();
        boolean containsTempImageIds = imgRequestDtoList.stream().anyMatch(imgRequestDto -> imgRequestDto.getTempFileId() != null);
        boolean hasTempImageIds = hasTempImageIds(imgRequestDtoList);
        if (hasDirectImages && containsTempImageIds) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
        if (hasDirectImages == hasTempImageIds) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }
        if (hasDirectImages && imgRequestDtoList.size() != safeImages.size()) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }

        if (itemRequestDto.getItemOptions() == null || itemRequestDto.getItemOptions().isEmpty()) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }

        List<Category> categories = getValidatedCategories(itemRequestDto.getCategoryIds());

        // 1. 상품 저장
        Item savedItem = getSavedItem(itemRequestDto, member);

        List<ItemImg> savedImages = Collections.emptyList();
        List<Long> consumedTempFileIds = Collections.emptyList();

        try {
            // 2. 이미지 저장
            if (hasDirectImages) {
                List<ItemImgUploadVO> imgUploadVOList = new ArrayList<>();
                for (int i = 0; i < safeImages.size(); i++) {
                    imgUploadVOList.add(new ItemImgUploadVO(imgRequestDtoList.get(i), safeImages.get(i)));
                }
                savedImages = itemImgService.saveItemImages(savedItem, imgUploadVOList);
            } else {
                List<Long> tempFileIds = extractTempFileIds(imgRequestDtoList);
                List<UploadFile> tempUploadFiles = tempUploadFileService.resolveOwnedTempFiles(id, tempFileIds);
                savedImages = itemImgService.saveItemImagesFromStoredFiles(savedItem, imgRequestDtoList, tempUploadFiles);
                consumedTempFileIds = tempFileIds;
            }

            // 3. 옵션 저장
            itemOptionService.saveItemOptions(savedItem, itemRequestDto.getItemOptions());

            // 4. 카테고리 저장
            List<ItemCategory> itemCategories = categories.stream()
                    .map(category -> new ItemCategory(savedItem, category))
                    .toList();
            itemCategoryRepository.saveAll(itemCategories);

            if (!consumedTempFileIds.isEmpty()) {
                tempUploadFileService.consumeOwnedTempFiles(id, consumedTempFileIds);
            }

            // 5. Response 생성 및 반환
            return itemMapper.toResponseDto(savedItem);
        } catch (Exception e) {
            if (hasDirectImages) {
                itemImgService.deleteStoredFiles(savedImages);
            }
            throw e;
        }
    }


    @Transactional
    public ItemResponseDto updateItem(Long memberId, Long itemId, ItemUpdateRequestDto dto, List<MultipartFile> newImages) throws IOException {
        // 1. 상품 조회
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ITEM_NOT_FOUND));
        validateSellerAuthority(memberId, item);
        List<MultipartFile> safeNewImages = newImages == null ? Collections.emptyList() : newImages;
        boolean hasDirectImages = !safeNewImages.isEmpty();

        // 2. 상품 기본 정보 업데이트
        item.updateItem(
                dto.getItemName(),
                dto.getPrice(),
                dto.getDiscount(),
                dto.getDeliveryFee(),
                dto.getOrigin(),
                dto.getItemLimit()
        );

        List<ItemImg> addedImages = Collections.emptyList();
        try {
            itemCategoryService.updateItemCategories(item, dto);

            // 3. 이미지 업데이트 (삭제 후 추가)
            addedImages = itemImgService.updateItemImages(item, dto, safeNewImages, memberId);

            // 4. 옵션 업데이트 (삭제 후 추가)
            itemOptionService.updateItemOptions(item, dto);

            Item updatedItem = itemRepository.save(item);

            // 6. Response 반환
            return itemMapper.toResponseDto(updatedItem);
        } catch (Exception e) {
            if (hasDirectImages) {
                itemImgService.deleteStoredFiles(addedImages);
            }
            throw e;
        }
    }

    public List<ItemImg> getItemImgs(Long id) {
        return itemImgRepository.findByItemId(id);
    }

    public List<ItemCategory> getItemCategories(Long id) {
        return itemCategoryRepository.findByItemId(id);
    }

    public List<ItemOption> getItemOptions(Long id) {
        return itemOptionRepository.findByItemId(id);
    }

    // 처음 등록할 때 save
    @Transactional
    private Item getSavedItem(ItemRequestDto itemRequestDto, Member seller) {
        Item item = itemMapper.toEntity(itemRequestDto); // DTO 엔티티 변환
        item.setSeller(seller); // 연관관계 편의 메서드
        return itemRepository.save(item);
    }

    private List<Category> getValidatedCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }

        LinkedHashSet<Long> uniqueCategoryIds = new LinkedHashSet<>(categoryIds);
        if (uniqueCategoryIds.size() != categoryIds.size()) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }

        return uniqueCategoryIds.stream()
                .map(categoryId -> categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new BusinessException(BusinessErrorCode.CATEGORY_NOT_FOUND)))
                .toList();
    }

    // 아이템 검색
    @Transactional(readOnly = true)
    public Page<ItemSearchResponseDto> searchItem(ItemSearchRequestDto itemSearchRequestDto, Pageable pageable) {
        return itemRepository.searchItems(itemSearchRequestDto, pageable);
    }

    // 많이 팔린 상품 조회
    @Transactional(readOnly = true)
    public List<ItemResponseDto> getPopularItem(int limit) {
        if (limit <= 0 || limit > 100) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }

        List<Long> popularItemIds = popularItemPort.getPopularItemIds(limit);
        if (popularItemIds.isEmpty()) {
            return List.of();
        }

        return itemRepository.findItemResponsesByIds(popularItemIds);
    }

    // 특정 아이템 조회
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public ItemResponseDto getItem(Long id) {
        return itemRepository.findItemResponseById(id)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ITEM_NOT_FOUND));
    }

    // 아이템 삭제
    @Transactional
    public void deleteItem(Long memberId, Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ITEM_NOT_FOUND));
        validateSellerAuthority(memberId, item);
        List<ItemImg> itemImages = itemImgRepository.findByItemId(id);
        itemImgService.deleteStoredFiles(itemImages);
        itemRepository.delete(item);
    }

    private void validateSellerAuthority(Long memberId, Item item) {
        if (!item.getSeller().getId().equals(memberId)) {
            throw new ForbiddenRequestException(ForbiddenErrorCode.MEMBER_UNAUTHORIZED);
        }
    }

    private boolean hasTempImageIds(List<ItemImgRequestDto> imgRequestDtoList) {
        return imgRequestDtoList.stream()
                .allMatch(imgRequestDto -> imgRequestDto.getTempFileId() != null);
    }

    private List<Long> extractTempFileIds(List<ItemImgRequestDto> imgRequestDtoList) {
        return imgRequestDtoList.stream()
                .map(ItemImgRequestDto::getTempFileId)
                .collect(Collectors.toList());
    }
}

