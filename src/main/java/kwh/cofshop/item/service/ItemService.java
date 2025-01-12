
package kwh.cofshop.item.service;

import kwh.cofshop.item.domain.Item;
import kwh.cofshop.item.domain.ItemOption;
import kwh.cofshop.item.dto.request.ItemCreateRequestDto;
import kwh.cofshop.item.dto.request.ItemRequestDto;
import kwh.cofshop.item.dto.request.ItemSearchRequestDto;
import kwh.cofshop.item.dto.response.ItemCreateResponseDto;
import kwh.cofshop.item.dto.response.ItemSearchResponseDto;
import kwh.cofshop.item.mapper.ItemCreateMapper;
import kwh.cofshop.item.mapper.ItemMapper;
import kwh.cofshop.item.mapper.ItemSearchMapper;
import kwh.cofshop.item.repository.ItemRepository;
import kwh.cofshop.member.domain.Member;
import kwh.cofshop.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemService { // 통합 Item 서비스
    private final ItemRepository itemRepository;

    // 매퍼 클래스
    private final ItemMapper itemMapper;
    private final ItemCreateMapper itemCreateMapper;
    private final ItemSearchMapper itemSearchMapper;

    // 연관관계 서비스
    private final ItemImgService itemImgService; // 이미지 서비스
    private final ItemOptionService itemOptionService; // 옵션 서비스


    // 아이템 등록
    @Transactional
    public ItemCreateResponseDto saveItem(ItemCreateRequestDto itemCreateRequestDto, Member seller) throws IOException {

       Item savedItem = getSavedItem(itemCreateRequestDto.getItemRequestDto(), seller);

       // 이미지 저장
       itemImgService.saveItemImages(savedItem,itemCreateRequestDto.getItemImgRequestDto());

       // 옵션 저장
       itemOptionService.saveItemOptions(savedItem, itemCreateRequestDto.getItemOptionRequestDto());

       ItemCreateResponseDto responseDto = itemCreateMapper.toResponseDto(savedItem);
       responseDto.getItemResponseDto().setEmail(seller.getEmail());
       // Response DTO 반환
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

