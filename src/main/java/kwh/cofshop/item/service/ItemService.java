package kwh.cofshop.item.service;

import kwh.cofshop.global.exception.BadRequestException;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.ForbiddenRequestException;
import kwh.cofshop.global.exception.errorcodes.BadRequestErrorCode;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.global.exception.errorcodes.ForbiddenErrorCode;
import kwh.cofshop.item.domain.*;
import kwh.cofshop.item.dto.request.ItemImgRequestDto;
import kwh.cofshop.item.dto.request.ItemRequestDto;
import kwh.cofshop.item.dto.request.ItemSearchRequestDto;
import kwh.cofshop.item.dto.request.ItemUpdateRequestDto;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import kwh.cofshop.item.dto.response.ItemSearchResponseDto;
import kwh.cofshop.item.mapper.ItemMapper;
import kwh.cofshop.item.mapper.ItemSearchMapper;
import kwh.cofshop.item.repository.*;
import kwh.cofshop.item.vo.ItemImgUploadVO;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.order.repository.OrderItemRepository;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemService { // 통합 Item 서비스
    private final ItemRepository itemRepository;

    // 매퍼 클래스
    private final ItemMapper itemMapper;
    private final ItemSearchMapper itemSearchMapper;

    // 연관관계 서비스
    private final ItemImgService itemImgService; // 이미지 서비스
    private final ItemOptionService itemOptionService; // 옵션 서비스
    private final ItemCategoryService itemCategoryService;

    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final ItemCategoryRepository itemCategoryRepository;
    private final ItemOptionRepository itemOptionRepository;
    private final ItemImgRepository itemImgRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public ItemResponseDto saveItem(ItemRequestDto itemRequestDto, Long id, List<MultipartFile> images) throws IOException {
        // 상품 등록자
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.MEMBER_NOT_FOUND));

        List<ItemImgRequestDto> imgRequestDtoList = itemRequestDto.getItemImgRequestDto();
        if (images == null || images.isEmpty()
                || imgRequestDtoList == null || imgRequestDtoList.isEmpty()
                || imgRequestDtoList.size() != images.size()) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }

        if (itemRequestDto.getItemOptionRequestDto() == null || itemRequestDto.getItemOptionRequestDto().isEmpty()) {
            throw new BadRequestException(BadRequestErrorCode.INPUT_INVALID_VALUE);
        }

        List<Category> categories = getValidatedCategories(itemRequestDto.getCategoryIds());

        // 1. 상품 저장
        Item savedItem = getSavedItem(itemRequestDto, member);

        // 2. DTO + 파일 VO 리스트 생성
        List<ItemImgUploadVO> imgUploadVOList = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            imgUploadVOList.add(new ItemImgUploadVO(imgRequestDtoList.get(i), images.get(i)));
        }

        // 3. 이미지 저장
        List<ItemImg> itemImgs = itemImgService.saveItemImages(savedItem, imgUploadVOList);

        // 4. 옵션 저장
        List<ItemOption> itemOptions = itemOptionService.saveItemOptions(savedItem, itemRequestDto.getItemOptionRequestDto());

        // 5. 카테고리 저장
        List<ItemCategory> itemCategories = categories.stream()
                .map(category -> new ItemCategory(savedItem, category))
                .toList();
        itemCategoryRepository.saveAll(itemCategories);

        // 6. Response 생성 및 반환
        ItemResponseDto responseDto = itemMapper.toResponseDto(savedItem);
        responseDto.setEmail(member.getEmail());
        return responseDto;
    }


    @Transactional
    public ItemResponseDto updateItem(Long memberId, Long itemId, ItemUpdateRequestDto dto, List<MultipartFile> newImages) throws IOException {
        // 1. 상품 조회
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ITEM_NOT_FOUND));
        validateSellerAuthority(memberId, item);

        // 2. 상품 기본 정보 업데이트
        item.updateItem(
                dto.getItemName(),
                dto.getPrice(),
                dto.getDiscount(),
                dto.getDeliveryFee(),
                dto.getOrigin(),
                dto.getItemLimit()
        );

        itemCategoryService.updateItemCategories(item, dto);

        // 3. 이미지 업데이트 (삭제 후 추가)
        itemImgService.updateItemImages(item, dto, newImages);

        // 4. 옵션 업데이트 (삭제 후 추가)
        itemOptionService.updateItemOptions(item, dto);

        Item updatedItem = itemRepository.save(item);

        // 6. Response 반환
        return itemMapper.toResponseDto(updatedItem);
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

        return categoryIds.stream()
                .map(categoryId -> categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new BusinessException(BusinessErrorCode.CATEGORY_NOT_FOUND)))
                .toList();
    }

    // 아이템 검색
    @Transactional(readOnly = true)
    public Page<ItemSearchResponseDto> searchItem(ItemSearchRequestDto itemSearchRequestDto, Pageable pageable) {
        Page<Item> itemPage = itemRepository.searchItems(itemSearchRequestDto, pageable);
        return itemPage.map(itemSearchMapper::toResponseDto);
    }

    // 많이 팔린 상품 조회
    public List<ItemResponseDto> getPopularItem(int limit) {
        List<Item> popularItems = orderItemRepository.getPopularItems(limit);
        return popularItems.stream()
                .map(itemMapper::toResponseDto)
                .toList();
    }

    // 특정 아이템 조회
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public ItemResponseDto getItem(Long id) {
        Item item = itemRepository.findById(id).orElseThrow(
                () -> new BusinessException(BusinessErrorCode.ITEM_NOT_FOUND));
        return itemMapper.toResponseDto(item);
    }

    // 아이템 삭제
    @Transactional
    public void deleteItem(Long memberId, Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ITEM_NOT_FOUND));
        validateSellerAuthority(memberId, item);
        itemRepository.delete(item);
    }

    private void validateSellerAuthority(Long memberId, Item item) {
        if (!item.getSeller().getId().equals(memberId)) {
            throw new ForbiddenRequestException(ForbiddenErrorCode.MEMBER_UNAUTHORIZED);
        }
    }


}

