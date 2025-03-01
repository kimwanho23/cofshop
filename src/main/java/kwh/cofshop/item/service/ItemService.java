
package kwh.cofshop.item.service;

import jakarta.persistence.EntityManager;
import kwh.cofshop.global.exception.BusinessException;
import kwh.cofshop.global.exception.errorcodes.BusinessErrorCode;
import kwh.cofshop.global.exception.errorcodes.ErrorCode;
import kwh.cofshop.item.domain.*;
import kwh.cofshop.item.dto.request.ItemImgRequestDto;
import kwh.cofshop.item.dto.request.ItemRequestDto;
import kwh.cofshop.item.dto.request.ItemSearchRequestDto;
import kwh.cofshop.item.dto.request.ItemUpdateRequestDto;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import kwh.cofshop.item.dto.response.ItemSearchResponseDto;
import kwh.cofshop.item.mapper.ItemImgMapper;
import kwh.cofshop.item.mapper.ItemMapper;
import kwh.cofshop.item.mapper.ItemOptionMapper;
import kwh.cofshop.item.mapper.ItemSearchMapper;
import kwh.cofshop.item.repository.*;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import kwh.cofshop.order.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemService { // 통합 Item 서비스
    private final ItemRepository itemRepository;

    // 매퍼 클래스
    private final ItemMapper itemMapper;
    private final ItemSearchMapper itemSearchMapper;
    private final ItemOptionMapper itemOptionMapper;
    private final ItemImgMapper itemImgMapper;

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
        Member member = memberRepository.findById(id).orElseThrow();

        // 1. 상품 저장
        Item savedItem = getSavedItem(itemRequestDto, member);

        // 2. DTO + 파일 매칭
        List<ItemImgRequestDto> imgRequestDto = itemRequestDto.getItemImgRequestDto();

        Map<ItemImgRequestDto, MultipartFile> imgMap = new LinkedHashMap<>();
        for (int i = 0; i < images.size(); i++) {
            imgMap.put(imgRequestDto.get(i), images.get(i));
        }

        //  3. 이미지 저장
        List<ItemImg> itemImgs = itemImgService.saveItemImages(savedItem, imgMap);

        //  4. 옵션 저장
        List<ItemOption> itemOptions = itemOptionService.saveItemOptions(savedItem, itemRequestDto.getItemOptionRequestDto());

        // 5. 카테고리 저장
        List<ItemCategory> itemCategories = new ArrayList<>();
        for (Long categoryId : itemRequestDto.getCategoryIds()) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new BusinessException(BusinessErrorCode.CATEGORY_NOT_FOUND));

            itemCategories.add(new ItemCategory(savedItem, category));
        }
        itemCategoryRepository.saveAll(itemCategories);

        //  6. Response 생성 및 반환
        ItemResponseDto responseDto = itemMapper.toResponseDto(savedItem);
        responseDto.setEmail(member.getEmail());
        return responseDto;
    }

    @Transactional
    public ItemResponseDto updateItem(Long itemId, ItemUpdateRequestDto dto, List<MultipartFile> newImages) throws IOException {
        // 1. 상품 조회
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ITEM_NOT_FOUND));

        // 2. 상품 기본 정보 업데이트
        item.updateItem(dto);

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

    // 아이템 검색
    @Transactional(readOnly = true)
    public Page<ItemSearchResponseDto> searchItem(ItemSearchRequestDto itemSearchRequestDto, Pageable pageable){
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
    public ItemResponseDto getItem(Long id){
        Item item = itemRepository.findById(id).orElseThrow(
                () -> new BusinessException(BusinessErrorCode.ITEM_NOT_FOUND));
        return itemMapper.toResponseDto(item);
    }

    // 아이템 삭제
    public void deleteItem(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BusinessErrorCode.ITEM_NOT_FOUND));
        itemRepository.delete(item);
    }


}

