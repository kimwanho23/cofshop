
package kwh.cofshop.item.service;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.dto.request.ItemImgRequestDto;
import kwh.cofshop.item.dto.request.ItemRequestDto;
import kwh.cofshop.item.dto.request.ItemSearchRequestDto;
import kwh.cofshop.item.dto.response.ItemResponseDto;
import kwh.cofshop.item.dto.response.ItemSearchResponseDto;
import kwh.cofshop.item.mapper.ItemMapper;
import kwh.cofshop.item.mapper.ItemSearchMapper;
import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.member.domain.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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


    @Transactional
    public ItemResponseDto saveItem(ItemRequestDto itemRequestDto, Member seller, List<MultipartFile> images) throws IOException {

        // 1. 상품 저장
        Item savedItem = getSavedItem(itemRequestDto, seller);

        // 2. DTO + 파일 매칭 (서비스에서 Map 생성)
        List<ItemImgRequestDto> imgRequestDto = itemRequestDto.getItemImgRequestDto();

        if (imgRequestDto.size() != images.size()) {
            throw new IllegalArgumentException("이미지 수와 DTO 수가 일치하지 않습니다.");
        }

        Map<ItemImgRequestDto, MultipartFile> imgMap = new LinkedHashMap<>();
        for (int i = 0; i < images.size(); i++) {
            imgMap.put(imgRequestDto.get(i), images.get(i));
        }

        //  3. 이미지 저장 (Map 사용)
        itemImgService.saveItemImages(savedItem, imgMap);

        //  4. 옵션 저장
        itemOptionService.saveItemOptions(savedItem, itemRequestDto.getItemOptionRequestDto());

        //  5. Response 생성 및 반환
        ItemResponseDto responseDto = itemMapper.toResponseDto(savedItem);
        responseDto.setEmail(seller.getEmail());
        return responseDto;
    }



    @Transactional
    private Item getSavedItem(ItemRequestDto itemRequestDto, Member seller) {
        Item item = itemMapper.toEntity(itemRequestDto); // DTO 엔티티 변환
        item.setSeller(seller); // 연관관계 편의 메서드
        return itemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public Page<ItemSearchResponseDto> searchItem(ItemSearchRequestDto itemSearchRequestDto, Pageable pageable){
        Page<Item> itemPage = itemRepository.findByItemName(itemSearchRequestDto.getItemName(), pageable);
        return itemPage.map(itemSearchMapper::toResponseDto);
    }

}

